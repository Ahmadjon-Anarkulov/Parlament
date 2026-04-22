package com.parlament.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private String token;
    private String username;
    private String mode = "long_polling";

    private Webhook webhook = new Webhook();
    private Admin admin = new Admin();

    @Data
    public static class Webhook {
        private boolean enabled = false;
        private String publicUrl;
        private String path = "/telegram/webhook";
    }

    @Data
    public static class Admin {
        private List<Long> ids = List.of();
        private boolean notifyOnOrder = true;
        private boolean notifyOnNewUser = true;
    }

    public boolean isWebhookMode() {
        return "webhook".equalsIgnoreCase(mode) || webhook.isEnabled();
    }
}
