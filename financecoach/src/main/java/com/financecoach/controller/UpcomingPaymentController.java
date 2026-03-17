package com.financecoach.controller;

import com.financecoach.dto.request.UpcomingPaymentRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.UpcomingPaymentResponse;
import com.financecoach.service.UpcomingPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Yaklaşan Ödemeler REST Controller
 * Base URL: /api/v1/payments
 *
 * GET  /api/v1/payments          → Tüm ödemeler
 * GET  /api/v1/payments/pending  → Ödenmemiş olanlar
 * GET  /api/v1/payments/urgent   → 3 gün içinde vadesi dolacaklar
 * POST /api/v1/payments          → Yeni ödeme ekle
 * PUT  /api/v1/payments/{id}/pay → Ödendi olarak işaretle
 * DELETE /api/v1/payments/{id}   → Sil
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class UpcomingPaymentController {

    private final UpcomingPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UpcomingPaymentResponse>>> getAllPayments() {
        List<UpcomingPaymentResponse> payments = paymentService.getUserPayments();
        return ResponseEntity.ok(
                ApiResponse.success(payments, payments.size() + " ödeme listelendi"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UpcomingPaymentResponse>>> getPendingPayments() {
        List<UpcomingPaymentResponse> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(
                ApiResponse.success(payments, payments.size() + " bekleyen ödeme"));
    }

    @GetMapping("/urgent")
    public ResponseEntity<ApiResponse<List<UpcomingPaymentResponse>>> getUrgentPayments() {
        List<UpcomingPaymentResponse> payments = paymentService.getUrgentPayments();
        return ResponseEntity.ok(
                ApiResponse.success(payments, payments.size() + " acil ödeme"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UpcomingPaymentResponse>> createPayment(
            @Valid @RequestBody UpcomingPaymentRequest request
    ) {
        log.debug("POST /v1/payments: {}", request.getTitle());
        UpcomingPaymentResponse created = paymentService.createPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Ödeme başarıyla eklendi"));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<UpcomingPaymentResponse>> markAsPaid(
            @PathVariable Long id
    ) {
        log.debug("PUT /v1/payments/{}/pay", id);
        UpcomingPaymentResponse updated = paymentService.markAsPaid(id);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Ödeme tamamlandı olarak işaretlendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Ödeme silindi"));
    }
}