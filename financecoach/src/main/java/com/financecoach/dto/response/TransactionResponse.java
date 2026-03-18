package com.financecoach.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financecoach.model.enums.PaymentMethod;
import com.financecoach.model.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String category;
    private String description;
    private LocalDate transactionDate;
    private Long userId;

    @JsonProperty("isFixed")
    private boolean isFixed;

    @JsonProperty("paymentMethod")
    private PaymentMethod paymentMethod;

    @JsonProperty("isRecurring")
    private boolean isRecurring;

    private Integer recurringDay;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}