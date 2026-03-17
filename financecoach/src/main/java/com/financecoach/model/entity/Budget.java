package com.financecoach.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "budgets",
        indexes = {
                @Index(name = "idx_budget_user_id", columnList = "user_id"),
                @Index(name = "idx_budget_user_category", columnList = "user_id, category")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_user_category",
                        columnNames = {"user_id", "category"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Bütçe kategorisi: "Market", "Ulaşım", "Fatura" vb. */
    @Column(nullable = false, length = 100)
    private String category;

    /** Aylık limit (TL) */
    @Column(name = "monthly_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    /** Bu ay harcanan tutar — her sorgu anında hesaplanır, entity'de tutulmaz */
    @Transient
    private BigDecimal currentSpent;

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