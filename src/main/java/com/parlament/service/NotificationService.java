package com.parlament.service;

import com.parlament.config.BotProperties;
import com.parlament.model.BotUser;
import com.parlament.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BotProperties props;

    public void notifyAdminsNewOrder(AbsSender bot, Order order) {
        if (!props.getAdmin().isNotifyOnOrder()) return;
        String text = buildOrderNotification(order);
        sendToAdmins(bot, text);
    }

    public void notifyAdminsNewUser(AbsSender bot, BotUser user) {
        if (!props.getAdmin().isNotifyOnNewUser()) return;
        String text = "👤 *Новый пользователь*\n\n" +
                "Имя: " + user.getDisplayName() + "\n" +
                (user.getUsername() != null ? "Username: @" + user.getUsername() + "\n" : "") +
                "ID: `" + user.getTelegramId() + "`";
        sendToAdmins(bot, text);
    }

    public void sendToUser(AbsSender bot, Long chatId, String text) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to send message to {}: {}", chatId, e.getMessage());
        }
    }

    private void sendToAdmins(AbsSender bot, String text) {
        props.getAdmin().getIds().forEach(adminId -> sendToUser(bot, adminId, text));
    }

    public static String buildOrderNotification(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 *Новый заказ #").append(order.getId()).append("*\n\n");
        sb.append("👤 *Клиент:* ").append(order.getUser().getDisplayName()).append("\n");
        if (order.getPhone() != null) sb.append("📞 *Телефон:* ").append(order.getPhone()).append("\n");
        sb.append("📦 *Тип:* ").append(order.getDeliveryType().getLabel()).append("\n");
        if (order.getDeliveryAddress() != null) sb.append("📍 *Адрес:* ").append(order.getDeliveryAddress()).append("\n");
        sb.append("\n*Состав заказа:*\n");
        order.getItems().forEach(item ->
                sb.append("• ").append(item.getProductName())
                        .append(" × ").append(item.getQuantity())
                        .append(" = ").append(String.format("%,.0f сум", item.getSubtotal()))
                        .append("\n")
        );
        sb.append("\n💰 *Итого:* ").append(order.formatTotal());
        if (order.getComment() != null && !order.getComment().isBlank()) {
            sb.append("\n💬 *Комментарий:* ").append(order.getComment());
        }
        return sb.toString();
    }
}
