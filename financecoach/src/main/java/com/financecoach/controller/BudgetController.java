package com.financecoach.controller;

import com.financecoach.dto.request.BudgetRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.BudgetResponse;
import com.financecoach.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bütçe Planlama REST Controller
 * Base URL: /api/v1/budgets
 *
 * POST   /api/v1/budgets          → Bütçe oluştur/güncelle
 * GET    /api/v1/budgets          → Tüm bütçeler (harcama durumu ile)
 * GET    /api/v1/budgets/alerts   → Aşılan/tehlikeli bütçeler
 * DELETE /api/v1/budgets/{id}     → Bütçe sil
 */
@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createOrUpdate(
            @Valid @RequestBody BudgetRequest request
    ) {
        log.debug("POST /v1/budgets: kategori='{}', limit={}", request.getCategory(), request.getMonthlyLimit());
        BudgetResponse budget = budgetService.createOrUpdateBudget(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(budget, "Bütçe kaydedildi"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets() {
        List<BudgetResponse> budgets = budgetService.getUserBudgets();
        return ResponseEntity.ok(
                ApiResponse.success(budgets, budgets.size() + " bütçe listelendi"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAlerts() {
        List<BudgetResponse> alerts = budgetService.getOverBudgetAlerts();
        return ResponseEntity.ok(
                ApiResponse.success(alerts, alerts.size() + " bütçe uyarısı"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bütçe silindi"));
    }
}