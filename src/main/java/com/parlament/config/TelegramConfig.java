package com.parlament.config;

import com.parlament.telegram.ParlamentBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramConfig {

    private final ParlamentBot bot;
    private final BotProperties props;

    @EventListener(ContextRefreshedEvent.class)
    public void registerBot() {
        if (props.isWebhookMode()) {
            log.info("Bot running in WEBHOOK mode — registration skipped (handled via controller)");
            return;
        }
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);
            log.info("✅ Bot @{} registered in LONG_POLLING mode", props.getUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register bot: {}", e.getMessage(), e);
            throw new RuntimeException("Bot registration failed", e);
        }
    }
}
