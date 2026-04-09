package com.parlament;

import com.parlament.bot.ParlamentBot;
import com.parlament.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Entry point for the Parlament Telegram Bot.
 * Initializes configuration, registers the bot, and starts listening for updates.
 */
public class  Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("╔══════════════════════════════════════╗");
        log.info("║     PARLAMENT BOT - Starting up      ║");
        log.info("║     Men's Classic Clothing Store     ║");
        log.info("╚══════════════════════════════════════╝");

        try {
            // Load configuration
            BotConfig config = BotConfig.load();
            log.info("Configuration loaded for bot: @{}", config.getBotUsername());

            // Register and start the bot
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            ParlamentBot bot = new ParlamentBot(config);
            botsApi.registerBot(bot);

            log.info("✅ Parlament Bot is running! Send /start to your bot.");

        } catch (TelegramApiException e) {
            log.error("❌ Failed to register the bot: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            log.error("❌ Unexpected error during startup: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
