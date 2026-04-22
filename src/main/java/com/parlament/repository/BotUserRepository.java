package com.parlament.repository;

import com.parlament.model.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    Optional<BotUser> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(u) FROM BotUser u WHERE u.admin = false AND u.banned = false")
    long countActiveUsers();
}
