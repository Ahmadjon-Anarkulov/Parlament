package com.parlament.bot;

import com.parlament.config.BotConfig;
import com.parlament.handler.CallbackHandler;
import com.parlament.handler.CommandHandler;
import com.parlament.handler.TextHandler;
import com.parlament.service.CartService;
import com.parlament.service.OrderService;
import com.parlament.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Main bot class. Wires together all handlers and services,
 * and delegates incoming updates to the appropriate handler.
 */
public class ParlamentBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(ParlamentBot.class);

    private final BotConfig config;

    private final CartService cartService;
    private final OrderService orderService;
    private final SessionService sessionService;

    private final CommandHandler commandHandler;
    private final CallbackHandler callbackHandler;
    private final TextHandler textHandler;

    public ParlamentBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;

        this.cartService    = new CartService();
        this.orderService   = new OrderService();
        this.sessionService = new SessionService();

        this.commandHandler  = new CommandHandler(this, cartService, orderService, sessionService);
        this.callbackHandler = new CallbackHandler(this, cartService, orderService, sessionService);
        this.textHandler     = new TextHandler(this, cartService, orderService, sessionService);
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                if (text.startsWith("/")) {
                    commandHandler.handle(update);
                } else {
                    textHandler.handle(update);
                }
            } else if (update.hasCallbackQuery()) {
                callbackHandler.handle(update);
            }
        } catch (Exception e) {
            log.error("Error processing update {}: {}", update.getUpdateId(), e.getMessage(), e);
        }
    }

    // ─────────────────────── Public Send Methods ───────────────────────

    public void sendText(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    public void sendPhoto(SendPhoto photo) {
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            log.error("Error sending photo: {}", e.getMessage());
        }
    }

    public void editMessage(EditMessageText editMessage) {
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Error editing message: {}", e.getMessage());
        }
    }

    public void answerCallback(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            if (text != null && !text.isBlank()) {
                answer.setText(text);
            }
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback: {}", e.getMessage());
        }
    }

    public void answerCallback(String callbackQueryId) {
        answerCallback(callbackQueryId, null);
    }
}