package com.financecoach.dto.request;

import com.financecoach.model.enums.PaymentCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingPaymentRequest {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir")
    private String title;

    @NotNull(message = "Kategori zorunludur")
    private PaymentCategory category;

    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    private BigDecimal amount;

    @NotNull(message = "Son ödeme tarihi zorunludur")
    private LocalDate dueDate;

    @DecimalMin(value = "0.01", message = "Kredi limiti 0'dan büyük olmalıdır")
    private BigDecimal creditLimit;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;

    private boolean isRecurring = false;
}