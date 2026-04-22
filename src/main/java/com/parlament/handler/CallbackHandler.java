package com.parlament.handler;

import com.parlament.model.*;
import com.parlament.service.*;
import com.parlament.util.BotSender;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private final CatalogService catalogService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;
    private final NotificationService notifier;

    public void handle(AbsSender bot, CallbackQuery cbq, BotUser user) {
        String data = cbq.getData();
        Long chatId = cbq.getMessage().getChatId();
        Integer msgId = cbq.getMessage().getMessageId();

        try {
            if (data.startsWith("cat:"))         handleCategory(bot, cbq, user, data);
            else if (data.startsWith("prod:"))    handleProduct(bot, cbq, user, data);
            else if (data.startsWith("add:"))     handleAddToCart(bot, cbq, user, data);
            else if (data.startsWith("cart:"))    handleCart(bot, cbq, user, data);
            else if (data.equals("checkout"))     handleCheckout(bot, cbq, user);
            else if (data.startsWith("back:"))    handleBack(bot, cbq, user, data);
            else if (data.startsWith("order:"))   handleOrderAction(bot, cbq, user, data);
            else if (data.startsWith("admin:"))   handleAdmin(bot, cbq, user, data);
            else if (data.equals("noop"))         BotSender.answerCallback(bot, cbq.getId(), "", false);
            else {
                BotSender.answerCallback(bot, cbq.getId(), "Неизвестное действие", false);
            }
        } catch (Exception e) {
            log.error("Callback error [{}]: {}", data, e.getMessage(), e);
            BotSender.answerCallback(bot, cbq.getId(), "Ошибка, попробуйте снова", true);
        }
    }

    private void handleCategory(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        Long catId = Long.parseLong(data.substring(4));
        var products = catalogService.getProductsByCategory(catId);
        var category = catalogService.findCategory(catId).orElseThrow();

        String text = category.getDisplayName() + "\n\nВыберите блюдо:";
        if (products.isEmpty()) text = "В этой категории пока нет блюд";

        BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                text, products.isEmpty() ? null : KeyboardFactory.products(products, catId));
        BotSender.answerCallback(bot, cbq.getId(), "", false);
    }

    private void handleProduct(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        Long prodId = Long.parseLong(data.substring(5));
        catalogService.findProduct(prodId).ifPresent(product -> {
            if (product.getFileId() != null) {
                BotSender.sendPhoto(bot, cbq.getMessage().getChatId(), product.getFileId(),
                        MessageFormatter.product(product), KeyboardFactory.productActions(product));
            } else {
                BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                        MessageFormatter.product(product), KeyboardFactory.productActions(product));
            }
        });
        BotSender.answerCallback(bot, cbq.getId(), "", false);
    }

    private void handleAddToCart(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        Long prodId = Long.parseLong(data.substring(4));
        cartService.addItem(user.getTelegramId(), prodId);
        long count = cartService.getCart(user.getTelegramId()).stream().mapToInt(CartItem::getQuantity).sum();
        BotSender.answerCallback(bot, cbq.getId(), "✅ Добавлено в корзину! (всего " + count + " шт.)", false);
    }

    private void handleCart(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        Long chatId = cbq.getMessage().getChatId();
        Integer msgId = cbq.getMessage().getMessageId();

        if (data.startsWith("cart:inc:")) {
            cartService.addItem(user.getTelegramId(), Long.parseLong(data.substring(9)));
        } else if (data.startsWith("cart:dec:")) {
            cartService.removeItem(user.getTelegramId(), Long.parseLong(data.substring(9)));
        } else if (data.startsWith("cart:del:")) {
            cartService.deleteItem(user.getTelegramId(), Long.parseLong(data.substring(9)));
        } else if (data.equals("cart:clear")) {
            cartService.clearCart(user.getTelegramId());
            BotSender.answerCallback(bot, cbq.getId(), "🗑 Корзина очищена", false);
        }

        List<CartItem> items = cartService.getCart(user.getTelegramId());
        BigDecimal total = cartService.getTotal(items);
        BotSender.edit(bot, chatId, msgId,
                MessageFormatter.cart(items, total), KeyboardFactory.cart(items));
        if (!data.equals("cart:clear")) BotSender.answerCallback(bot, cbq.getId(), "", false);
    }

    private void handleCheckout(AbsSender bot, CallbackQuery cbq, BotUser user) {
        if (cartService.isEmpty(user.getTelegramId())) {
            BotSender.answerCallback(bot, cbq.getId(), "Корзина пуста!", true);
            return;
        }
        userService.setState(user.getTelegramId(), "CHECKOUT_DELIVERY");
        BotSender.answerCallback(bot, cbq.getId(), "", false);
        BotSender.send(bot, cbq.getMessage().getChatId(),
                "🚚 *Оформление заказа*\n\nВыберите способ получения:",
                KeyboardFactory.deliveryTypeKeyboard());
    }

    private void handleBack(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        String target = data.substring(5);
        if (target.equals("cats")) {
            var cats = catalogService.getActiveCategories();
            BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                    "🍽️ *Выберите категорию:*", KeyboardFactory.categories(cats));
        } else if (target.startsWith("cat:")) {
            Long catId = Long.parseLong(target.substring(4));
            var products = catalogService.getProductsByCategory(catId);
            var cat = catalogService.findCategory(catId).orElseThrow();
            BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                    cat.getDisplayName(), KeyboardFactory.products(products, catId));
        }
        BotSender.answerCallback(bot, cbq.getId(), "", false);
    }

    private void handleOrderAction(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        // order:refresh:ID
        if (data.startsWith("order:refresh:")) {
            Long orderId = Long.parseLong(data.substring(14));
            orderService.findById(orderId).ifPresent(o ->
                    BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                            MessageFormatter.orderCard(o), KeyboardFactory.orderStatus(o.getId()))
            );
        }
        BotSender.answerCallback(bot, cbq.getId(), "Обновлено", false);
    }

    private void handleAdmin(AbsSender bot, CallbackQuery cbq, BotUser user, String data) {
        if (!user.isAdmin()) {
            BotSender.answerCallback(bot, cbq.getId(), "⛔ Нет доступа", true);
            return;
        }

        if (data.startsWith("admin:status:")) {
            // admin:status:ORDER_ID:STATUS
            String[] parts = data.split(":");
            Long orderId = Long.parseLong(parts[2]);
            Order.Status newStatus = Order.Status.valueOf(parts[3]);
            Order updated = orderService.updateStatus(orderId, newStatus, null);

            // Notify user
            notifier.sendToUser(bot, updated.getUser().getTelegramId(),
                    "📦 *Статус заказа #" + orderId + " изменён*\n" +
                            "Новый статус: " + newStatus.getLabel());

            BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                    NotificationService.buildOrderNotification(updated),
                    KeyboardFactory.adminOrderActions(orderId, newStatus));
            BotSender.answerCallback(bot, cbq.getId(), "✅ Статус обновлён", false);

        } else if (data.startsWith("admin:prod:toggle:")) {
            Long prodId = Long.parseLong(data.substring(18));
            catalogService.toggleProductAvailability(prodId);
            BotSender.answerCallback(bot, cbq.getId(), "✅ Доступность обновлена", false);
            catalogService.findProduct(prodId).ifPresent(p ->
                    BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                            MessageFormatter.product(p), KeyboardFactory.adminProductActions(p))
            );

        } else if (data.startsWith("admin:prod:list")) {
            var products = catalogService.getActiveCategories().stream()
                    .flatMap(c -> catalogService.getProductsByCategory(c.getId()).stream())
                    .toList();
            StringBuilder sb = new StringBuilder("🛍️ *Все товары:*\n\n");
            products.forEach(p -> sb.append(p.isAvailable() ? "🟢" : "🔴")
                    .append(" ").append(p.getName()).append(" — ").append(p.formatPrice()).append("\n"));
            BotSender.edit(bot, cbq.getMessage().getChatId(), cbq.getMessage().getMessageId(),
                    sb.toString(), null);
            BotSender.answerCallback(bot, cbq.getId(), "", false);
        }
    }
}
