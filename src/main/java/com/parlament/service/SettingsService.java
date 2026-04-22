package com.parlament.service;

import com.parlament.model.BotSetting;
import com.parlament.repository.BotSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final BotSettingRepository repo;

    @Cacheable(value = "settings", key = "#key")
    public String get(String key, String defaultValue) {
        return repo.findById(key).map(BotSetting::getValue).orElse(defaultValue);
    }

    public String get(String key) { return get(key, ""); }

    @Transactional
    @CacheEvict(value = "settings", key = "#key")
    public void set(String key, String value) {
        BotSetting setting = repo.findById(key)
                .orElse(BotSetting.builder().key(key).build());
        setting.setValue(value);
        repo.save(setting);
    }

    public String getWelcomeMessage() {
        return get("welcome_message", "Добро пожаловать! 🍽️");
    }

    public String getShopName() { return get("shop_name", "Парламент"); }
    public String getShopPhone() { return get("shop_phone", ""); }
    public String getShopAddress() { return get("shop_address", ""); }
    public String getShopHours() { return get("shop_hours", ""); }

    public boolean isMaintenanceMode() {
        return "true".equalsIgnoreCase(get("maintenance_mode", "false"));
    }

    public boolean isBotActive() {
        return "true".equalsIgnoreCase(get("bot_active", "true"));
    }
}
