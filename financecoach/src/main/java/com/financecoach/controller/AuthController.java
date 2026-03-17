package com.financecoach.controller;

import com.financecoach.dto.request.LoginRequest;
import com.financecoach.dto.request.RegisterRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.AuthResponse;
import com.financecoach.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kimlik doğrulama REST Controller.
 * Base URL: /v1/auth
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Yeni kullanıcı kaydı.
     * POST /v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.debug("POST /v1/auth/register isteği alındı: email={}", request.getEmail());
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "Kayıt başarıyla tamamlandı"));
    }

    /**
     * Mevcut kullanıcı girişi.
     * POST /v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.debug("POST /v1/auth/login isteği alındı: email={}", request.getEmail());
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "Giriş başarılı")
        );
    }
}