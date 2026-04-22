package com.parlament.service;

import com.parlament.config.BotProperties;
import com.parlament.model.BotUser;
import com.parlament.repository.BotUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final BotUserRepository userRepo;
    private final BotProperties props;

    @Transactional
    public BotUser getOrCreate(User tgUser) {
        return userRepo.findByTelegramId(tgUser.getId())
                .map(u -> updateActivity(u, tgUser))
                .orElseGet(() -> create(tgUser));
    }

    private BotUser updateActivity(BotUser user, User tgUser) {
        user.setLastActivityAt(LocalDateTime.now());
        user.setFirstName(tgUser.getFirstName());
        user.setLastName(tgUser.getLastName());
        if (tgUser.getUserName() != null) user.setUsername(tgUser.getUserName());
        return userRepo.save(user);
    }

    private BotUser create(User tgUser) {
        boolean isAdmin = props.getAdmin().getIds().contains(tgUser.getId());
        BotUser user = BotUser.builder()
                .telegramId(tgUser.getId())
                .username(tgUser.getUserName())
                .firstName(tgUser.getFirstName())
                .lastName(tgUser.getLastName())
                .languageCode(tgUser.getLanguageCode())
                .admin(isAdmin)
                .build();
        BotUser saved = userRepo.save(user);
        log.info("New user registered: {} ({})", saved.getDisplayName(), saved.getTelegramId());
        return saved;
    }

    @Transactional
    public void setState(Long telegramId, String state) {
        userRepo.findByTelegramId(telegramId).ifPresent(u -> {
            u.setState(state);
            userRepo.save(u);
        });
    }

    @Transactional(readOnly = true)
    public Optional<BotUser> findByTelegramId(Long telegramId) {
        return userRepo.findByTelegramId(telegramId);
    }

    public boolean isAdmin(Long telegramId) {
        return props.getAdmin().getIds().contains(telegramId)
                || userRepo.findByTelegramId(telegramId).map(BotUser::isAdmin).orElse(false);
    }

    @Transactional
    public void ban(Long telegramId, String reason) {
        userRepo.findByTelegramId(telegramId).ifPresent(u -> {
            u.setBanned(true);
            u.setBanReason(reason);
            userRepo.save(u);
        });
    }

    @Transactional
    public void unban(Long telegramId) {
        userRepo.findByTelegramId(telegramId).ifPresent(u -> {
            u.setBanned(false);
            u.setBanReason(null);
            userRepo.save(u);
        });
    }

    @Transactional
    public void savePhone(Long telegramId, String phone) {
        userRepo.findByTelegramId(telegramId).ifPresent(u -> {
            u.setPhone(phone);
            userRepo.save(u);
        });
    }

    public long countTotal() { return userRepo.count(); }
    public long countActive() { return userRepo.countActiveUsers(); }
    public long countToday() {
        return userRepo.countByCreatedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
