package com.parlament.repository;

import com.parlament.model.BotSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotSettingRepository extends JpaRepository<BotSetting, String> {
}
