package com.parlament.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotSetting {

    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
