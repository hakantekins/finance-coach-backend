package com.financecoach.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Kullanıcı profil güncelleme isteği.
 * PUT /api/v1/users/profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Ad soyad 2-100 karakter arasında olmalıdır")
    private String fullName;

    @DecimalMin(value = "0.0", message = "Aylık gelir negatif olamaz")
    @Digits(integer = 10, fraction = 2, message = "Geçersiz tutar formatı")
    private BigDecimal monthlyIncome;

    @DecimalMin(value = "0.0", message = "Tasarruf hedefi negatif olamaz")
    @Digits(integer = 10, fraction = 2, message = "Geçersiz tutar formatı")
    private BigDecimal monthlySavingsGoal;
}