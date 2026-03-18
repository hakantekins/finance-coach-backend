package com.financecoach.service.impl;

import com.financecoach.dto.response.DashboardResponse;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.TransactionType;
import com.financecoach.repository.TransactionRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * DashboardService implementasyonu.
 * <p>
 * Tek sorumluluğu: Transaction verilerinden dashboard metriklerini hesaplayıp DTO'ya döndürmek.
 * Hesaplama burada yapılıyor; aylık metrikler current calendar month aralığına göre hesaplanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Sadece okuma
public class DashboardServiceImpl extends BaseAuthService implements DashboardService {

    private final TransactionRepository transactionRepository;

    @Override
    public DashboardResponse getDashboard() {
        User currentUser = getAuthenticatedUser();

        // Current calendar month hesapları (timezone karmaşasını LocalDate ile minimize eder)
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        log.debug(
                "Dashboard hesaplanıyor: userId={}, month={}/{}",
                currentUser.getId(), monthStart, monthEnd
        );

        BigDecimal totalIncome = transactionRepository
                .findTotalByUserIdAndType(currentUser.getId(), TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository
                .findTotalByUserIdAndType(currentUser.getId(), TransactionType.EXPENSE);

        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        BigDecimal currentMonthExpenseTotal = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(
                        currentUser.getId(),
                        TransactionType.EXPENSE,
                        monthStart,
                        monthEnd
                );
        currentMonthExpenseTotal = currentMonthExpenseTotal != null ? currentMonthExpenseTotal : BigDecimal.ZERO;

        BigDecimal optimizableExpense = transactionRepository
                .findOptimizableExpenseByUserIdAndDateRange(
                        currentUser.getId(),
                        monthStart,
                        monthEnd
                );
        optimizableExpense = optimizableExpense != null ? optimizableExpense : BigDecimal.ZERO;

        BigDecimal potentialMonthlySavings = optimizableExpense
                .multiply(BigDecimal.valueOf(0.30))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal monthlySavingsGoal = currentUser.getMonthlySavingsGoal() != null
                ? currentUser.getMonthlySavingsGoal()
                : BigDecimal.ZERO;
        BigDecimal declaredMonthlyIncome = currentUser.getMonthlyIncome() != null
                ? currentUser.getMonthlyIncome()
                : BigDecimal.ZERO;

        // 0-100 aralığında clamp
        double savingsGoalProgressPct;
        if (monthlySavingsGoal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal net = declaredMonthlyIncome.subtract(currentMonthExpenseTotal);
            BigDecimal pct = net
                    .divide(monthlySavingsGoal, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            double pctD = pct.doubleValue();
            savingsGoalProgressPct = Math.max(0.0, Math.min(100.0, pctD));
        } else {
            savingsGoalProgressPct = 0.0;
        }

        // Akıllı harcama skoru: 100 - (optimizlenebilir / toplam gider) * 100
        double smartSpendingScore;
        if (currentMonthExpenseTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratioPct = optimizableExpense
                    .divide(currentMonthExpenseTotal, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            BigDecimal score = BigDecimal.valueOf(100).subtract(ratioPct);
            smartSpendingScore = score.doubleValue();
            smartSpendingScore = Math.max(0.0, Math.min(100.0, smartSpendingScore));
        } else {
            // Bu ay gider yoksa skoru "iyi" kabul et
            smartSpendingScore = 100.0;
        }

        return DashboardResponse.builder()
                .userId(currentUser.getId())
                .fullName(currentUser.getFullName())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .currentMonthExpenseTotal(currentMonthExpenseTotal)
                .potentialMonthlySavings(potentialMonthlySavings)
                .monthlySavingsGoal(monthlySavingsGoal)
                .declaredMonthlyIncome(declaredMonthlyIncome)
                .savingsGoalProgressPct(savingsGoalProgressPct)
                .smartSpendingScore(smartSpendingScore)
                .build();
    }
}