package com.financecoach.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * PUT /api/v1/users/password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Şifre en az bir büyük harf, bir küçük harf ve bir rakam içermelidir"
    )
    private String newPassword;

    @NotBlank(message = "Şifre tekrarı boş olamaz")
    private String confirmPassword;
}