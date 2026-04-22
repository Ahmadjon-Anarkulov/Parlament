package com.parlament.service;

import com.parlament.model.BotUser;
import com.parlament.model.CartItem;
import com.parlament.model.Product;
import com.parlament.repository.BotUserRepository;
import com.parlament.repository.CartItemRepository;
import com.parlament.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final BotUserRepository userRepo;

    @Transactional(readOnly = true)
    public List<CartItem> getCart(Long telegramId) {
        return userRepo.findByTelegramId(telegramId)
                .map(u -> cartRepo.findByUserIdOrderByAddedAtAsc(u.getId()))
                .orElse(List.of());
    }

    @Transactional
    public void addItem(Long telegramId, Long productId) {
        BotUser user = userRepo.findByTelegramId(telegramId).orElseThrow();
        Product product = productRepo.findById(productId).orElseThrow();

        cartRepo.findByUserIdAndProductId(user.getId(), productId)
                .ifPresentOrElse(
                        item -> { item.setQuantity(item.getQuantity() + 1); cartRepo.save(item); },
                        () -> cartRepo.save(CartItem.builder().user(user).product(product).quantity(1).build())
                );
    }

    @Transactional
    public void removeItem(Long telegramId, Long productId) {
        userRepo.findByTelegramId(telegramId).ifPresent(user ->
                cartRepo.findByUserIdAndProductId(user.getId(), productId).ifPresent(item -> {
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        cartRepo.save(item);
                    } else {
                        cartRepo.delete(item);
                    }
                })
        );
    }

    @Transactional
    public void deleteItem(Long telegramId, Long productId) {
        userRepo.findByTelegramId(telegramId).ifPresent(user ->
                cartRepo.findByUserIdAndProductId(user.getId(), productId)
                        .ifPresent(cartRepo::delete)
        );
    }

    @Transactional
    public void clearCart(Long telegramId) {
        userRepo.findByTelegramId(telegramId).ifPresent(u ->
                cartRepo.deleteAllByUserId(u.getId())
        );
    }

    public BigDecimal getTotal(List<CartItem> items) {
        return items.stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount(List<CartItem> items) {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isEmpty(Long telegramId) {
        return userRepo.findByTelegramId(telegramId)
                .map(u -> cartRepo.countByUserId(u.getId()) == 0)
                .orElse(true);
    }
}
