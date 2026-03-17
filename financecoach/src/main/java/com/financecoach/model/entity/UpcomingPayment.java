package com.financecoach.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financecoach.model.enums.PaymentCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "upcoming_payments",
        indexes = {
                @Index(name = "idx_up_user_id",  columnList = "user_id"),
                @Index(name = "idx_up_due_date", columnList = "due_date"),
                @Index(name = "idx_up_is_paid",  columnList = "is_paid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @JsonProperty("isPaid")
    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean isPaid = false;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(length = 500)
    private String description;

    @JsonProperty("isRecurring")
    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean isRecurring = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}