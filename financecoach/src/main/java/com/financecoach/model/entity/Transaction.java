package com.financecoach.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financecoach.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @JsonProperty("isFixed")
    @Column(name = "is_fixed", nullable = false)
    @Builder.Default
    private boolean isFixed = false;
    /**
     * Tekrarlayan işlem mi? (Maaş, kira, Netflix vb.)
     * true ise her ayın recurringDay gününde otomatik oluşturulur.
     */
    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean isRecurring = false;

    /**
     * Ayın kaçında tekrarlansın? (1-31)
     * Sadece isRecurring=true ise anlamlı.
     * Örn: 1 = her ayın 1'i, 15 = her ayın 15'i
     */
    @Column(name = "recurring_day")
    private Integer recurringDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.transactionDate == null) {
            this.transactionDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}