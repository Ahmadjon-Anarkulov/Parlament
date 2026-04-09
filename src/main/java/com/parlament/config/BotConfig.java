package com.parlament.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and holds bot configuration.
 * Reads from bot.properties file, with fallback to environment variables.
 */
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);
    private static final String PROPERTIES_FILE = "bot.properties";

    private final String botToken;
    private final String botUsername;

    private BotConfig(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    /**
     * Loads configuration from bot.properties or environment variables.
     * Environment variables take priority over properties file.
     */
    public static BotConfig load() {
        Properties props = new Properties();

        // Try to load from classpath properties file
        try (InputStream is = BotConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                props.load(is);
                log.debug("Loaded configuration from {}", PROPERTIES_FILE);
            } else {
                log.warn("Could not find {} in classpath", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", PROPERTIES_FILE, e.getMessage());
        }

        // Environment variables override properties file
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isBlank()) {
            token = props.getProperty("BOT_TOKEN", "");
        }

        String username = System.getenv("BOT_USERNAME");
        if (username == null || username.isBlank()) {
            username = props.getProperty("BOT_USERNAME", "ParlamentStoreBot");
        }

        if (token.isBlank() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            throw new IllegalStateException(
                "BOT_TOKEN is not configured! Set it in src/main/resources/bot.properties " +
                "or as the BOT_TOKEN environment variable."
            );
        }

        return new BotConfig(token, username);
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }
}
