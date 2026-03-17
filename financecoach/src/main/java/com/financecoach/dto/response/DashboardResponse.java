package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard endpoint'inden istemciye dönen özet DTO'su.
 * ID tipi Long olarak güncellendi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private Long userId; // UUID -> Long yapıldı
    private String fullName;

    // ── Tüm Zamanlara Ait Finansal Özet ─────────────────────────────────────
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;

    // ── Bu Aya Ait Harcama Metrikleri ────────────────────────────────────────
    private BigDecimal currentMonthExpenseTotal;
    private BigDecimal potentialMonthlySavings;

    // ── Hedef Bilgileri ──────────────────────────────────────────────────────
    private BigDecimal monthlySavingsGoal;
    private BigDecimal declaredMonthlyIncome;

    // ── Hesaplanan Skor / Yüzdeler ───────────────────────────────────────────
    private Double savingsGoalProgressPct; // BigDecimal -> Double yapıldı (Entity ile uyum)
    private Double smartSpendingScore;     // BigDecimal -> Double yapıldı
}