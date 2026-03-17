package com.financecoach.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financecoach.model.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotNull(message = "İşlem tutarı zorunludur")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    @Digits(integer = 10, fraction = 2, message = "Tutar en fazla 10 tam, 2 ondalık basamak içerebilir")
    private BigDecimal amount;

    @NotNull(message = "İşlem tipi zorunludur (INCOME veya EXPENSE)")
    private TransactionType type;

    @NotBlank(message = "Kategori boş bırakılamaz")
    @Size(max = 100, message = "Kategori en fazla 100 karakter olabilir")
    private String category;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    @PastOrPresent(message = "İşlem tarihi gelecekte olamaz")
    private LocalDate transactionDate;

    /**
     * Sabit gider mi? (Kira, fatura, abonelik vb.)
     * true ise AI Coach bu harcamayı tasarruf analizine dahil etmez.
     * Default: false
     */
    @JsonProperty("isFixed")
    private boolean isFixed = false;
    @JsonProperty("isRecurring")
    private boolean isRecurring = false;

    /** Ayın kaçında tekrarlansın? (1-31) */
    private Integer recurringDay;
}