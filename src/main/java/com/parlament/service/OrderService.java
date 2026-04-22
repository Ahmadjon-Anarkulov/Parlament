package com.parlament.service;

import com.parlament.model.*;
import com.parlament.repository.BotUserRepository;
import com.parlament.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final BotUserRepository userRepo;
    private final CartService cartService;

    @Transactional
    public Order createOrder(Long telegramId, Order.DeliveryType deliveryType,
                              String address, String phone, String comment) {
        BotUser user = userRepo.findByTelegramId(telegramId).orElseThrow();
        List<CartItem> cartItems = cartService.getCart(telegramId);

        if (cartItems.isEmpty()) throw new IllegalStateException("Cart is empty");

        BigDecimal total = cartService.getTotal(cartItems);

        Order order = Order.builder()
                .user(user)
                .status(Order.Status.PENDING)
                .totalAmount(total)
                .deliveryType(deliveryType)
                .deliveryAddress(address)
                .phone(phone)
                .comment(comment)
                .build();

        List<OrderItem> items = cartItems.stream()
                .map(ci -> OrderItem.builder()
                        .order(order)
                        .product(ci.getProduct())
                        .productName(ci.getProduct().getName())
                        .price(ci.getProduct().getPrice())
                        .quantity(ci.getQuantity())
                        .subtotal(ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .build())
                .toList();
        order.setItems(items);

        Order saved = orderRepo.save(order);

        // Update user stats
        user.setOrdersCount(user.getOrdersCount() + 1);
        user.setTotalSpent(user.getTotalSpent().add(total));
        userRepo.save(user);

        // Clear cart
        cartService.clearCart(telegramId);

        log.info("Order #{} created for user {} — {} сум", saved.getId(), user.getDisplayName(), total);
        return saved;
    }

    @Transactional
    public Order updateStatus(Long orderId, Order.Status newStatus, String adminComment) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        if (adminComment != null) order.setAdminComment(adminComment);
        if (newStatus == Order.Status.COMPLETED) order.setCompletedAt(LocalDateTime.now());
        return orderRepo.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long telegramId) {
        return userRepo.findByTelegramId(telegramId)
                .map(u -> orderRepo.findByUserIdOrderByCreatedAtDesc(u.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Order> getPendingOrders(int page) {
        return orderRepo.findByStatusOrderByCreatedAtDesc(Order.Status.PENDING, PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(int page) {
        return orderRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 10));
    }

    // Stats
    public long countPending()  { return orderRepo.countByStatus(Order.Status.PENDING); }
    public long countTotal()    { return orderRepo.count(); }
    public long countToday()    { return orderRepo.countByCreatedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay()); }
    public BigDecimal totalRevenue() { return orderRepo.sumCompletedRevenue(); }
    public BigDecimal revenueToday() {
        return orderRepo.sumRevenueAfter(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
