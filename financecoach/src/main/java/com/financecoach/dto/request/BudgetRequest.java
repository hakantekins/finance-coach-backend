package com.financecoach.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @NotBlank(message = "Kategori boş olamaz")
    @Size(max = 100, message = "Kategori en fazla 100 karakter olabilir")
    private String category;

    @NotNull(message = "Aylık limit zorunludur")
    @DecimalMin(value = "1.00", message = "Limit en az 1 TL olmalıdır")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal monthlyLimit;
}