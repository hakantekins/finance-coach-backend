package com.financecoach.controller;

import com.financecoach.dto.request.ChangePasswordRequest;
import com.financecoach.dto.request.UpdateProfileRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.AuthResponse;
import com.financecoach.model.entity.User;
import com.financecoach.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Kullanıcı profil işlemleri.
 * Base URL: /api/v1/users
 *
 * GET /api/v1/users/profile   → Profil bilgilerini getir
 * PUT /api/v1/users/profile   → Profil güncelle
 * PUT /api/v1/users/password  → Şifre değiştir
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse>> getProfile() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build(),
                "Profil bilgileri getirildi"
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = getAuthenticatedUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getMonthlyIncome() != null) {
            user.setMonthlyIncome(request.getMonthlyIncome());
        }
        if (request.getMonthlySavingsGoal() != null) {
            user.setMonthlySavingsGoal(request.getMonthlySavingsGoal());
        }

        User saved = userRepository.save(user);
        log.info("Profil güncellendi: userId={}", saved.getId());

        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .email(saved.getEmail())
                        .fullName(saved.getFullName())
                        .build(),
                "Profil başarıyla güncellendi"
        ));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = getAuthenticatedUser();

        // 1. Yeni şifre ile tekrarı eşleşiyor mu?
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Yeni şifre ve tekrarı eşleşmiyor")
            );
        }

        // 2. Mevcut şifre doğru mu?
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Mevcut şifre hatalı")
            );
        }

        // 3. Yeni şifre eskiyle aynı mı?
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Yeni şifre mevcut şifreyle aynı olamaz")
            );
        }

        // 4. Şifreyi güncelle
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Şifre değiştirildi: userId={}", user.getId());

        return ResponseEntity.ok(ApiResponse.success(null, "Şifre başarıyla değiştirildi"));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}