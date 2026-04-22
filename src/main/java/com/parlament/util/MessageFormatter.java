package com.parlament.util;

import com.parlament.model.BotUser;
import com.parlament.model.CartItem;
import com.parlament.model.Order;
import com.parlament.model.Product;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessageFormatter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static String product(Product p) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(escape(p.getName())).append("*\n\n");
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append(escape(p.getDescription())).append("\n\n");
        }
        if (p.hasDiscount()) {
            sb.append("~~").append(p.getOldPrice()).append(" сум~~ ");
        }
        sb.append("💰 *").append(p.formatPrice()).append("*\n");
        if (!p.isAvailable()) sb.append("\n❌ _Временно недоступно_");
        return sb.toString();
    }

    public static String cart(List<CartItem> items, BigDecimal total) {
        if (items.isEmpty()) return "🛒 Ваша корзина пуста\n\nПерейдите в *Меню*, чтобы добавить товары.";
        StringBuilder sb = new StringBuilder("🛒 *Ваша корзина*\n\n");
        for (CartItem item : items) {
            BigDecimal sub = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            sb.append("• ").append(escape(item.getProduct().getName()))
                    .append(" × ").append(item.getQuantity())
                    .append(" = *").append(String.format("%,.0f сум", sub)).append("*\n");
        }
        sb.append("\n💰 *Итого: ").append(String.format("%,.0f сум", total)).append("*");
        return sb.toString();
    }

    public static String orderCreated(Order order) {
        return "✅ *Заказ #" + order.getId() + " принят!*\n\n" +
                "💰 Сумма: *" + order.formatTotal() + "*\n" +
                "📦 " + order.getDeliveryType().getLabel() + "\n\n" +
                "Мы свяжемся с вами в ближайшее время.\n" +
                "Статус заказа можно проверить в разделе *Мои заказы*.";
    }

    public static String orderCard(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Заказ #").append(order.getId()).append("*\n");
        sb.append("📅 ").append(order.getCreatedAt().format(DT)).append("\n");
        sb.append("🔖 Статус: ").append(order.getStatus().getLabel()).append("\n");
        sb.append("📦 ").append(order.getDeliveryType().getLabel()).append("\n");
        if (order.getDeliveryAddress() != null) sb.append("📍 ").append(escape(order.getDeliveryAddress())).append("\n");
        sb.append("\n");
        order.getItems().forEach(item ->
                sb.append("• ").append(escape(item.getProductName()))
                        .append(" × ").append(item.getQuantity())
                        .append(" = ").append(String.format("%,.0f сум", item.getSubtotal())).append("\n")
        );
        sb.append("\n💰 *Итого: ").append(order.formatTotal()).append("*");
        if (order.getAdminComment() != null) sb.append("\n\n💬 _").append(escape(order.getAdminComment())).append("_");
        return sb.toString();
    }

    public static String profile(BotUser user, int ordersCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("👤 *Профиль*\n\n");
        sb.append("Имя: ").append(escape(user.getDisplayName())).append("\n");
        if (user.getUsername() != null) sb.append("Username: @").append(user.getUsername()).append("\n");
        if (user.getPhone() != null) sb.append("📞 Телефон: ").append(user.getPhone()).append("\n");
        sb.append("\n📋 Заказов: *").append(ordersCount).append("*\n");
        sb.append("💰 Потрачено: *").append(String.format("%,.0f сум", user.getTotalSpent())).append("*\n");
        sb.append("\n📅 С нами с: ").append(user.getCreatedAt().format(DT));
        return sb.toString();
    }

    public static String adminStats(long users, long activeUsers, long todayUsers,
                                     long orders, long pendingOrders, long todayOrders,
                                     BigDecimal revenue, BigDecimal todayRevenue) {
        return "📊 *Статистика бота*\n\n" +
                "👥 *Пользователи*\n" +
                "• Всего: " + users + "\n" +
                "• Активных: " + activeUsers + "\n" +
                "• Сегодня новых: " + todayUsers + "\n\n" +
                "📦 *Заказы*\n" +
                "• Всего: " + orders + "\n" +
                "• Ожидают: " + pendingOrders + "\n" +
                "• Сегодня: " + todayOrders + "\n\n" +
                "💰 *Выручка*\n" +
                "• Всего: " + String.format("%,.0f сум", revenue) + "\n" +
                "• Сегодня: " + String.format("%,.0f сум", todayRevenue);
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_").replace("*", "\\*").replace("[", "\\[").replace("`", "\\`");
    }
}
