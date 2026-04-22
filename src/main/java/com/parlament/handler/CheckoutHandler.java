package com.parlament.handler;

import com.parlament.model.BotUser;
import com.parlament.model.Order;
import com.parlament.service.*;
import com.parlament.util.BotSender;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Handles the multi-step checkout flow:
 * CHECKOUT_DELIVERY → CHECKOUT_PHONE → CHECKOUT_ADDRESS (if delivery) → CHECKOUT_COMMENT → done
 */
@Component
@RequiredArgsConstructor
public class CheckoutHandler {

    private final UserService userService;
    private final OrderService orderService;
    private final NotificationService notifier;
    private final CartService cartService;

    // Temp storage key prefix in user state: "CHECKOUT_DELIVERY", "CHECKOUT_PHONE:DELIVERY_TYPE", etc.

    public void handle(AbsSender bot, Message msg, BotUser user, String state) {
        String text = msg.getText();
        Long chatId = msg.getChatId();

        // Handle contact share
        if (msg.hasContact()) {
            if (state.startsWith("CHECKOUT_PHONE")) {
                String phone = msg.getContact().getPhoneNumber();
                userService.savePhone(user.getTelegramId(), phone);
                continueAfterPhone(bot, chatId, user, state, phone);
            }
            return;
        }

        switch (state) {
            case "CHECKOUT_DELIVERY" -> handleDeliveryChoice(bot, chatId, user, text);
            default -> {
                if (state.startsWith("CHECKOUT_PHONE")) handlePhoneInput(bot, chatId, user, state, text);
                else if (state.startsWith("CHECKOUT_ADDRESS")) handleAddressInput(bot, chatId, user, state, text);
                else if (state.startsWith("CHECKOUT_COMMENT")) handleCommentInput(bot, chatId, user, state, text);
            }
        }
    }

    private void handleDeliveryChoice(AbsSender bot, Long chatId, BotUser user, String text) {
        Order.DeliveryType type;
        if (text.contains("Самовывоз")) {
            type = Order.DeliveryType.PICKUP;
        } else if (text.contains("Доставка")) {
            type = Order.DeliveryType.DELIVERY;
        } else if (text.contains("Отмена")) {
            cancel(bot, chatId, user);
            return;
        } else {
            BotSender.send(bot, chatId, "Пожалуйста, выберите тип доставки:", KeyboardFactory.deliveryTypeKeyboard());
            return;
        }

        // Ask for phone
        userService.setState(user.getTelegramId(), "CHECKOUT_PHONE:" + type.name());
        String prompt = "📱 *Введите ваш номер телефона*\nили поделитесь через кнопку ниже:";
        BotSender.send(bot, chatId, prompt, KeyboardFactory.contactKeyboard());
    }

    private void handlePhoneInput(AbsSender bot, Long chatId, BotUser user, String state, String text) {
        if (isCancelled(text)) { cancel(bot, chatId, user); return; }
        String phone = text.trim();
        userService.savePhone(user.getTelegramId(), phone);
        continueAfterPhone(bot, chatId, user, state, phone);
    }

    private void continueAfterPhone(AbsSender bot, Long chatId, BotUser user, String state, String phone) {
        String[] parts = state.split(":", 2);
        Order.DeliveryType type = Order.DeliveryType.valueOf(parts.length > 1 ? parts[1] : "PICKUP");

        if (type == Order.DeliveryType.DELIVERY) {
            userService.setState(user.getTelegramId(), "CHECKOUT_ADDRESS:" + type.name() + ":" + phone);
            BotSender.send(bot, chatId, "📍 *Введите адрес доставки:*", KeyboardFactory.cancelOnly());
        } else {
            userService.setState(user.getTelegramId(), "CHECKOUT_COMMENT:" + type.name() + "::" + phone);
            BotSender.send(bot, chatId,
                    "💬 *Добавить комментарий к заказу?*\nНапишите его или отправьте \"-\" чтобы пропустить:",
                    KeyboardFactory.cancelOnly());
        }
    }

    private void handleAddressInput(AbsSender bot, Long chatId, BotUser user, String state, String text) {
        if (isCancelled(text)) { cancel(bot, chatId, user); return; }
        // state = "CHECKOUT_ADDRESS:DELIVERY_TYPE:PHONE"
        String[] parts = state.split(":", 3);
        String deliveryType = parts.length > 1 ? parts[1] : "DELIVERY";
        String phone = parts.length > 2 ? parts[2] : "";

        userService.setState(user.getTelegramId(), "CHECKOUT_COMMENT:" + deliveryType + ":" + text.trim() + ":" + phone);
        BotSender.send(bot, chatId,
                "💬 *Добавить комментарий?*\nНапишите его или отправьте \"-\" чтобы пропустить:",
                KeyboardFactory.cancelOnly());
    }

    private void handleCommentInput(AbsSender bot, Long chatId, BotUser user, String state, String text) {
        if (isCancelled(text)) { cancel(bot, chatId, user); return; }
        // state = "CHECKOUT_COMMENT:DELIVERY_TYPE:ADDRESS:PHONE"
        String[] parts = state.split(":", 4);
        Order.DeliveryType type = Order.DeliveryType.valueOf(parts.length > 1 ? parts[1] : "PICKUP");
        String address = parts.length > 2 ? parts[2] : null;
        String phone = parts.length > 3 ? parts[3] : null;
        String comment = "-".equals(text.trim()) ? null : text.trim();

        try {
            Order order = orderService.createOrder(
                    user.getTelegramId(), type,
                    type == Order.DeliveryType.DELIVERY ? address : null,
                    phone, comment
            );

            userService.setState(user.getTelegramId(), "MAIN");
            BotSender.send(bot, chatId,
                    MessageFormatter.orderCreated(order),
                    user.isAdmin() ? KeyboardFactory.adminMenu() : KeyboardFactory.mainMenu());

            // Notify admins
            notifier.notifyAdminsNewOrder(bot, order);

        } catch (IllegalStateException e) {
            BotSender.send(bot, chatId, "❌ Корзина пуста!",
                    user.isAdmin() ? KeyboardFactory.adminMenu() : KeyboardFactory.mainMenu());
            userService.setState(user.getTelegramId(), "MAIN");
        }
    }

    private void cancel(AbsSender bot, Long chatId, BotUser user) {
        userService.setState(user.getTelegramId(), "MAIN");
        BotSender.send(bot, chatId, "❌ Заказ отменён",
                user.isAdmin() ? KeyboardFactory.adminMenu() : KeyboardFactory.mainMenu());
    }

    private boolean isCancelled(String text) {
        return "❌ Отмена".equals(text) || "/cancel".equals(text) || "◀️ Назад".equals(text);
    }
}
