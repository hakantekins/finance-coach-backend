package com.financecoach.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * POST /auth/register endpoint'i için istek gövdesi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Ad soyad boş olamaz")
    @Size(min = 2, max = 100, message = "Ad soyad 2-100 karakter arasında olmalıdır")
    private String fullName;

    @NotBlank(message = "E-posta adresi boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 255, message = "E-posta adresi en fazla 255 karakter olabilir")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, max = 100, message = "Şifre en az 8, en fazla 100 karakter olmalıdır")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Şifre en az bir büyük harf, bir küçük harf ve bir rakam içermelidir"
    )
    private String password;

    /**
     * Opsiyonel: Kayıt sırasında aylık gelir girilebilir.
     */
    @DecimalMin(value = "0.0", message = "Aylık gelir negatif olamaz")
    @Digits(integer = 10, fraction = 2, message = "Geçersiz tutar formatı")
    private BigDecimal monthlyIncome;

    /**
     * Opsiyonel: Kayıt sırasında aylık tasarruf hedefi girilebilir.
     */
    @DecimalMin(value = "0.0", message = "Tasarruf hedefi negatif olamaz")
    @Digits(integer = 10, fraction = 2, message = "Geçersiz tutar formatı")
    private BigDecimal monthlySavingsGoal;
}