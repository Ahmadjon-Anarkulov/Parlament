package com.parlament.telegram;

import com.parlament.config.BotProperties;
import com.parlament.handler.CallbackHandler;
import com.parlament.handler.CheckoutHandler;
import com.parlament.handler.CommandHandler;
import com.parlament.handler.TextHandler;
import com.parlament.model.BotUser;
import com.parlament.service.NotificationService;
import com.parlament.service.SettingsService;
import com.parlament.service.UserService;
import com.parlament.util.BotSender;
import com.parlament.util.KeyboardFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class ParlamentBot extends TelegramLongPollingBot {

    private final BotProperties props;
    private final UserService userService;
    private final CommandHandler commandHandler;
    private final TextHandler textHandler;
    private final CallbackHandler callbackHandler;
    private final CheckoutHandler checkoutHandler;
    private final NotificationService notifier;
    private final SettingsService settings;

    public ParlamentBot(BotProperties props,
                        UserService userService,
                        CommandHandler commandHandler,
                        TextHandler textHandler,
                        CallbackHandler callbackHandler,
                        CheckoutHandler checkoutHandler,
                        NotificationService notifier,
                        SettingsService settings) {
        super(props.getToken());
        this.props = props;
        this.userService = userService;
        this.commandHandler = commandHandler;
        this.textHandler = textHandler;
        this.callbackHandler = callbackHandler;
        this.checkoutHandler = checkoutHandler;
        this.notifier = notifier;
        this.settings = settings;
    }

    @Override
    public String getBotUsername() {
        return props.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Unhandled error processing update: {}", e.getMessage(), e);
        }
    }

    private void handleMessage(Message msg) {
        if (!msg.hasText() && !msg.hasContact()) return;
        if (msg.getFrom() == null) return;

        BotUser user = userService.getOrCreate(msg.getFrom());
        Long chatId = msg.getChatId();

        // Ban check
        if (user.isBanned()) {
            BotSender.send(this, chatId, "⛔ Ваш аккаунт заблокирован.\nПричина: " +
                    (user.getBanReason() != null ? user.getBanReason() : "Нарушение правил"));
            return;
        }

        // Maintenance mode check (skip for admins)
        if (settings.isMaintenanceMode() && !user.isAdmin()) {
            BotSender.send(this, chatId,
                    "🔧 Бот временно на техническом обслуживании. Попробуйте позже.");
            return;
        }

        // New user notification
        if (user.getOrdersCount() == 0 && isFirstMessage(user)) {
            notifier.notifyAdminsNewUser(this, user);
        }

        String state = user.getState() != null ? user.getState() : "MAIN";

        // Checkout flow takes priority
        if (state.startsWith("CHECKOUT_")) {
            checkoutHandler.handle(this, msg, user, state);
            return;
        }

        // Commands
        if (msg.hasText() && msg.getText().startsWith("/")) {
            commandHandler.handle(this, msg, user);
            return;
        }

        // Regular text
        textHandler.handle(this, msg, user);
    }

    private void handleCallback(CallbackQuery cbq) {
        if (cbq.getFrom() == null) return;
        BotUser user = userService.getOrCreate(cbq.getFrom());

        if (user.isBanned()) {
            BotSender.answerCallback(this, cbq.getId(), "⛔ Аккаунт заблокирован", true);
            return;
        }

        callbackHandler.handle(this, cbq, user);
    }

    private boolean isFirstMessage(BotUser user) {
        // New user if created within last 5 seconds
        return user.getCreatedAt() != null &&
                java.time.Duration.between(user.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds() < 5;
    }
}
