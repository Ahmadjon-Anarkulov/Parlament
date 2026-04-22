package com.parlament.handler;

import com.parlament.model.BotUser;
import com.parlament.service.OrderService;
import com.parlament.service.SettingsService;
import com.parlament.service.UserService;
import com.parlament.util.BotSender;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
public class CommandHandler {

    private final UserService userService;
    private final OrderService orderService;
    private final SettingsService settings;

    public void handle(AbsSender bot, Message msg, BotUser user) {
        String cmd = msg.getText().split(" ")[0].toLowerCase().replace("@" + getUsername(bot), "");
        switch (cmd) {
            case "/start" -> handleStart(bot, msg, user);
            case "/menu"  -> handleMenu(bot, msg, user);
            case "/admin" -> handleAdmin(bot, msg, user);
            case "/help"  -> handleHelp(bot, msg, user);
            case "/cancel" -> handleCancel(bot, msg, user);
            case "/orders" -> handleOrders(bot, msg, user);
            default -> BotSender.send(bot, msg.getChatId(), "Неизвестная команда. Попробуйте /help");
        }
    }

    private void handleStart(AbsSender bot, Message msg, BotUser user) {
        userService.setState(user.getTelegramId(), "MAIN");
        String welcome = settings.getWelcomeMessage();
        BotSender.send(bot, msg.getChatId(),
                "👋 Привет, *" + user.getDisplayName() + "*!\n\n" + welcome,
                user.isAdmin() ? KeyboardFactory.adminMenu() : KeyboardFactory.mainMenu());
    }

    private void handleMenu(AbsSender bot, Message msg, BotUser user) {
        userService.setState(user.getTelegramId(), "MAIN");
        BotSender.send(bot, msg.getChatId(),
                "Выберите раздел:", KeyboardFactory.mainMenu());
    }

    private void handleAdmin(AbsSender bot, Message msg, BotUser user) {
        if (!userService.isAdmin(user.getTelegramId())) {
            BotSender.send(bot, msg.getChatId(), "⛔ Нет доступа");
            return;
        }
        userService.setState(user.getTelegramId(), "ADMIN");
        BotSender.send(bot, msg.getChatId(),
                "🔧 *Панель администратора*\nВыберите раздел:",
                KeyboardFactory.adminMenu());
    }

    private void handleHelp(AbsSender bot, Message msg, BotUser user) {
        String help = "ℹ️ *Помощь*\n\n" +
                "🍽️ *Меню* — просмотр и заказ блюд\n" +
                "🛒 *Корзина* — ваша корзина\n" +
                "📋 *Мои заказы* — история заказов\n" +
                "📞 *Контакты* — наши контакты\n\n" +
                "*Команды:*\n" +
                "/start — Главное меню\n" +
                "/menu — Меню ресторана\n" +
                "/orders — Мои заказы\n" +
                "/help — Эта справка";
        BotSender.send(bot, msg.getChatId(), help, KeyboardFactory.mainMenu());
    }

    private void handleCancel(AbsSender bot, Message msg, BotUser user) {
        userService.setState(user.getTelegramId(), "MAIN");
        BotSender.send(bot, msg.getChatId(), "Действие отменено", KeyboardFactory.mainMenu());
    }

    private void handleOrders(AbsSender bot, Message msg, BotUser user) {
        var orders = orderService.getUserOrders(user.getTelegramId());
        if (orders.isEmpty()) {
            BotSender.send(bot, msg.getChatId(), "У вас пока нет заказов", KeyboardFactory.mainMenu());
            return;
        }
        orders.stream().limit(5).forEach(o ->
                BotSender.send(bot, msg.getChatId(), MessageFormatter.orderCard(o),
                        KeyboardFactory.orderStatus(o.getId()))
        );
    }

    private String getUsername(AbsSender bot) {
        try { return bot.getMe().getUserName(); } catch (Exception e) { return ""; }
    }
}
