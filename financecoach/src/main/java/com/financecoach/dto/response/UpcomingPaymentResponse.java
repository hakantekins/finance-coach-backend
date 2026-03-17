package com.financecoach.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financecoach.model.enums.PaymentCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingPaymentResponse {

    private Long id;
    private String title;
    private PaymentCategory category;
    private BigDecimal amount;
    private LocalDate dueDate;

    @JsonProperty("isPaid")
    private boolean isPaid;

    private BigDecimal creditLimit;
    private String description;

    @JsonProperty("isRecurring")
    private boolean isRecurring;

    /** Kaç gün kaldı — backend hesaplıyor, frontend direkt kullanır */
    private long daysUntilDue;

    /** Son ödeme tarihi geçti mi? */
    @JsonProperty("isOverdue")
    private boolean isOverdue;

    /** 3 gün veya daha az kaldı mı? Uyarı göstermek için */
    @JsonProperty("isUrgent")
    private boolean isUrgent;

    private OffsetDateTime createdAt;
}