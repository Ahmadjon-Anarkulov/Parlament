package com.parlament.handler;

import com.parlament.bot.ParlamentBot;
import com.parlament.service.CartService;
import com.parlament.service.OrderService;
import com.parlament.service.SessionService;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private final ParlamentBot bot;
    private final CartService cartService;
    private final OrderService orderService;
    private final SessionService sessionService;

    public CommandHandler(ParlamentBot bot, CartService cartService,
                          OrderService orderService, SessionService sessionService) {
        this.bot = bot;
        this.cartService = cartService;
        this.orderService = orderService;
        this.sessionService = sessionService;
    }

    public void handle(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();
        String text = message.getText().trim();
        String command = text.split("\\s+")[0].toLowerCase();

        log.debug("Команда от пользователя {}: {}", userId, command);

        switch (command) {
            case "/start"   -> handleStart(chatId, message.getFrom().getFirstName(), userId);
            case "/help"    -> handleHelp(chatId);
            case "/catalog" -> bot.sendText(buildCatalogMessage(chatId));
            case "/cart"    -> bot.sendText(buildCartMessage(chatId, userId));
            case "/orders"  -> bot.sendText(buildOrdersMessage(chatId, userId));
            default         -> bot.sendText(buildMessage(chatId, MessageFormatter.unknownCommandMessage()));
        }
    }

    private void handleStart(long chatId, String firstName, long userId) {
        sessionService.resetCheckout(userId);
        SendMessage msg = buildMessage(chatId, MessageFormatter.welcomeMessage(firstName));
        msg.setReplyMarkup(KeyboardFactory.mainMenuKeyboard());
        bot.sendText(msg);
    }

    private void handleHelp(long chatId) {
        String helpText = "🎩 <b>Parlament Bot — Помощь</b>\n\n"
                + "<b>Команды:</b>\n"
                + "/start — Вернуться в главное меню\n"
                + "/catalog — Открыть каталог\n"
                + "/cart — Открыть корзину\n"
                + "/orders — История заказов\n"
                + "/help — Показать это сообщение";
        bot.sendText(buildMessage(chatId, helpText));
    }

    private SendMessage buildCatalogMessage(long chatId) {
        SendMessage msg = buildMessage(chatId, MessageFormatter.catalogMessage());
        msg.setReplyMarkup(KeyboardFactory.catalogKeyboard());
        return msg;
    }

    private SendMessage buildCartMessage(long chatId, long userId) {
        var items = cartService.getCartItems(userId);
        SendMessage msg;
        if (items.isEmpty()) {
            msg = buildMessage(chatId, MessageFormatter.emptyCartMessage());
            msg.setReplyMarkup(KeyboardFactory.cartKeyboard(false));
        } else {
            msg = buildMessage(chatId, MessageFormatter.cartMessage(items, cartService.getCartTotal(userId)));
            msg.setReplyMarkup(KeyboardFactory.cartWithItemsKeyboard(items));
        }
        return msg;
    }

    private SendMessage buildOrdersMessage(long chatId, long userId) {
        var orders = orderService.getOrdersForUser(userId);
        SendMessage msg;
        if (orders.isEmpty()) {
            msg = buildMessage(chatId, MessageFormatter.noOrdersMessage());
        } else {
            msg = buildMessage(chatId, MessageFormatter.orderHistoryMessage(orders));
        }
        msg.setReplyMarkup(KeyboardFactory.backToMainKeyboard());
        return msg;
    }

    private SendMessage buildMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setParseMode("HTML");
        msg.disableWebPagePreview();
        return msg;
    }
}
