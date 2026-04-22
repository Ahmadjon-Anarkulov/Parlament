package com.parlament.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Order {

    public enum Status {
        PENDING("⏳ Ожидает"),
        CONFIRMED("✅ Подтверждён"),
        PREPARING("👨‍🍳 Готовится"),
        READY("🔔 Готов"),
        DELIVERING("🚴 В пути"),
        COMPLETED("✔️ Выполнен"),
        CANCELLED("❌ Отменён");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum DeliveryType {
        PICKUP("🏪 Самовывоз"),
        DELIVERY("🚴 Доставка");

        private final String label;
        DeliveryType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private BotUser user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryType deliveryType = DeliveryType.PICKUP;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column
    private String phone;

    @Column(name = "payment_method")
    @Builder.Default
    private String paymentMethod = "CASH";

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public String formatTotal() {
        return String.format("%,.0f сум", totalAmount);
    }
}
