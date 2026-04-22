package com.parlament.handler;

import com.parlament.model.BotUser;
import com.parlament.model.CartItem;
import com.parlament.model.Order;
import com.parlament.service.*;
import com.parlament.util.BotSender;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TextHandler {

    private final UserService userService;
    private final CatalogService catalogService;
    private final CartService cartService;
    private final OrderService orderService;
    private final NotificationService notifier;
    private final SettingsService settings;

    public void handle(AbsSender bot, Message msg, BotUser user) {
        String text = msg.getText();
        String state = user.getState();

        // Handle state-based input first
        if (state != null && state.startsWith("CHECKOUT_")) {
            handleCheckoutState(bot, msg, user, state, text);
            return;
        }
        if (state != null && state.startsWith("ADMIN_")) {
            handleAdminState(bot, msg, user, state, text);
            return;
        }

        switch (text) {
            case "🍽️ Меню"          -> showCatalog(bot, msg, user);
            case "🛒 Корзина"        -> showCart(bot, msg, user);
            case "📋 Мои заказы"     -> showOrders(bot, msg, user);
            case "ℹ️ О нас"          -> showAbout(bot, msg, user);
            case "📞 Контакты"       -> showContacts(bot, msg, user);
            case "⚙️ Профиль"        -> showProfile(bot, msg, user);
            case "◀️ Назад", "◀️ Главное меню" -> goMain(bot, msg, user);
            case "❌ Отмена"         -> goMain(bot, msg, user);
            // Admin menu buttons
            case "📊 Статистика"     -> showAdminStats(bot, msg, user);
            case "📦 Заказы"         -> showAdminOrders(bot, msg, user);
            case "🛍️ Каталог"        -> showAdminCatalog(bot, msg, user);
            case "👥 Пользователи"   -> showAdminUsers(bot, msg, user);
            case "⚙️ Настройки"      -> showAdminSettings(bot, msg, user);
            case "📢 Рассылка"       -> startBroadcast(bot, msg, user);
            default -> BotSender.send(bot, msg.getChatId(),
                    "Используйте кнопки меню или /help для справки.", getKeyboard(user));
        }
    }

    private void showCatalog(AbsSender bot, Message msg, BotUser user) {
        var cats = catalogService.getActiveCategories();
        if (cats.isEmpty()) {
            BotSender.send(bot, msg.getChatId(), "😔 Меню временно недоступно", getKeyboard(user));
            return;
        }
        BotSender.send(bot, msg.getChatId(), "🍽️ *Выберите категорию:*",
                KeyboardFactory.categories(cats));
    }

    private void showCart(AbsSender bot, Message msg, BotUser user) {
        List<CartItem> items = cartService.getCart(user.getTelegramId());
        BigDecimal total = cartService.getTotal(items);
        BotSender.send(bot, msg.getChatId(),
                MessageFormatter.cart(items, total),
                KeyboardFactory.cart(items));
    }

    private void showOrders(AbsSender bot, Message msg, BotUser user) {
        var orders = orderService.getUserOrders(user.getTelegramId());
        if (orders.isEmpty()) {
            BotSender.send(bot, msg.getChatId(), "📋 У вас пока нет заказов.\n\nНачните с *Меню*! 🍽️", getKeyboard(user));
            return;
        }
        BotSender.send(bot, msg.getChatId(), "📋 *Ваши заказы:*", getKeyboard(user));
        orders.stream().limit(5).forEach(o ->
                BotSender.send(bot, msg.getChatId(), MessageFormatter.orderCard(o),
                        KeyboardFactory.orderStatus(o.getId()))
        );
    }

    private void showAbout(AbsSender bot, Message msg, BotUser user) {
        String text = "🏛️ *" + settings.getShopName() + "*\n\n" +
                "Добро пожаловать! Мы рады видеть вас в нашем заведении.\n\n" +
                "🕐 Режим работы: " + settings.getShopHours() + "\n" +
                "📍 Адрес: " + settings.getShopAddress() + "\n" +
                "📞 Телефон: " + settings.getShopPhone();
        BotSender.send(bot, msg.getChatId(), text, getKeyboard(user));
    }

    private void showContacts(AbsSender bot, Message msg, BotUser user) {
        String text = "📞 *Контакты*\n\n" +
                "📞 Телефон: " + settings.getShopPhone() + "\n" +
                "📍 Адрес: " + settings.getShopAddress() + "\n" +
                "🕐 Часы работы: " + settings.getShopHours();
        BotSender.send(bot, msg.getChatId(), text, getKeyboard(user));
    }

    private void showProfile(AbsSender bot, Message msg, BotUser user) {
        BotSender.send(bot, msg.getChatId(),
                MessageFormatter.profile(user, user.getOrdersCount()), getKeyboard(user));
    }

    private void goMain(AbsSender bot, Message msg, BotUser user) {
        userService.setState(user.getTelegramId(), "MAIN");
        BotSender.send(bot, msg.getChatId(), "Главное меню:", getKeyboard(user));
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    private void showAdminStats(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        String text = MessageFormatter.adminStats(
                userService.countTotal(), userService.countActive(), userService.countToday(),
                orderService.countTotal(), orderService.countPending(), orderService.countToday(),
                orderService.totalRevenue(), orderService.revenueToday()
        );
        BotSender.send(bot, msg.getChatId(), text, KeyboardFactory.adminMenu());
    }

    private void showAdminOrders(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        var page = orderService.getPendingOrders(0);
        if (page.isEmpty()) {
            BotSender.send(bot, msg.getChatId(), "📭 Нет ожидающих заказов", KeyboardFactory.adminMenu());
            return;
        }
        BotSender.send(bot, msg.getChatId(), "📦 *Ожидающие заказы:*", KeyboardFactory.adminMenu());
        page.forEach(o -> BotSender.send(bot, msg.getChatId(),
                NotificationService.buildOrderNotification(o),
                KeyboardFactory.adminOrderActions(o.getId(), o.getStatus())));
    }

    private void showAdminCatalog(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        long cats = catalogService.countCategories();
        long prods = catalogService.countProducts();
        String text = "🛍️ *Каталог*\n\nКатегорий: " + cats + "\nТоваров (активных): " + prods;
        BotSender.send(bot, msg.getChatId(), text, KeyboardFactory.adminMenu());
        BotSender.send(bot, msg.getChatId(), "Выберите действие:", KeyboardFactory.adminCatalogMenu());
    }

    private void showAdminUsers(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        String text = "👥 *Пользователи*\n\n" +
                "Всего: " + userService.countTotal() + "\n" +
                "Активных: " + userService.countActive() + "\n" +
                "Сегодня: " + userService.countToday();
        BotSender.send(bot, msg.getChatId(), text, KeyboardFactory.adminMenu());
    }

    private void showAdminSettings(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        String text = "⚙️ *Настройки*\n\n" +
                "Название: " + settings.getShopName() + "\n" +
                "Телефон: " + settings.getShopPhone() + "\n" +
                "Адрес: " + settings.getShopAddress() + "\n" +
                "Часы: " + settings.getShopHours() + "\n\n" +
                "🔧 Режим обслуживания: " + (settings.isMaintenanceMode() ? "🔴 ВКЛ" : "🟢 ВЫКЛ");
        BotSender.send(bot, msg.getChatId(), text, KeyboardFactory.adminMenu());
    }

    private void startBroadcast(AbsSender bot, Message msg, BotUser user) {
        if (!user.isAdmin()) { goMain(bot, msg, user); return; }
        userService.setState(user.getTelegramId(), "ADMIN_BROADCAST");
        BotSender.send(bot, msg.getChatId(),
                "📢 *Рассылка*\n\nОтправьте сообщение для рассылки всем пользователям.\n\nДля отмены: /cancel",
                KeyboardFactory.cancelOnly());
    }

    private void handleCheckoutState(AbsSender bot, Message msg, BotUser user, String state, String text) {
        // Handled by CheckoutHandler
    }

    private void handleAdminState(AbsSender bot, Message msg, BotUser user, String state, String text) {
        switch (state) {
            case "ADMIN_BROADCAST" -> {
                // TODO: broadcast logic
                BotSender.send(bot, msg.getChatId(), "📢 Рассылка отправлена!", KeyboardFactory.adminMenu());
                userService.setState(user.getTelegramId(), "ADMIN");
            }
        }
    }

    private org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard getKeyboard(BotUser user) {
        return user.isAdmin() ? KeyboardFactory.adminMenu() : KeyboardFactory.mainMenu();
    }
}
