package com.financecoach.exception;

import com.financecoach.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Tüm uygulama genelinde hataları yakalar ve tutarlı ApiResponse formatında döner.
 *
 * NEDEN GEREKLİ:
 * - Validation hatası olduğunda Spring 400 + ham JSON döner, frontend parse edemez
 * - BadCredentials olduğunda Spring 401 döner ama body boştur
 * - ResourceNotFoundException için 404 dönmesi gerekir
 * - Beklenmedik hatalar için 500 + anlamlı mesaj
 *
 * EKLEME NEDEN ZORUNLU:
 * Frontend login sayfasında hata mesajlarını göstermek için
 * backend'den anlamlı yanıt gelmesi gerekiyor.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @Valid validation hatalarını yakalar.
     * Örn: email formatı yanlış, şifre kısa, required field boş
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        log.warn("Validation hatası: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Giriş verileri geçersiz"));
    }

    /**
     * Yanlış email/şifre kombinasyonu.
     * AuthServiceImpl'den fırlatılır.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Kimlik doğrulama hatası: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("E-posta veya şifre hatalı"));
    }

    /**
     * Kayıt bulunamadı (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * İş mantığı hataları (zaten kayıtlı email vb.)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime hatası: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Beklenmeyen tüm hatalar için fallback.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Beklenmeyen hata: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Sunucu hatası oluştu, lütfen tekrar deneyin"));
    }
}