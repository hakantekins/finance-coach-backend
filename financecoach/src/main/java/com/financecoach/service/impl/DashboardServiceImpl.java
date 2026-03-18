package com.financecoach.service.impl;

import com.financecoach.dto.response.DashboardResponse;
import com.financecoach.exception.ResourceNotFoundException;
import com.financecoach.model.entity.DashboardSummary;
import com.financecoach.model.entity.User;
import com.financecoach.repository.DashboardRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DashboardService implementasyonu.
 * <p>
 * Tek sorumluluğu: VIEW'dan veri çek → DTO'ya map et → döndür.
 * Hesaplama yoktur; tüm aggregation PostgreSQL VIEW'ında yapılmıştır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // VIEW'dan okuma; yazma transaction'ı açılmaz
public class DashboardServiceImpl extends BaseAuthService implements DashboardService {

    private final DashboardRepository dashboardRepository;

    @Override
    public DashboardResponse getDashboard() {
        User currentUser = getAuthenticatedUser();

        log.debug("Dashboard verisi çekiliyor: userId={}", currentUser.getId());

        java.util.Optional<DashboardSummary> summaryOpt = dashboardRepository.findByUserId(currentUser.getId());

        if (summaryOpt.isPresent()) {
            // Veritabanında (VIEW) kayıt varsa normal şekilde çevir ve gönder
            log.debug("Dashboard verisi başarıyla çekildi: userId={}", currentUser.getId());
            return mapToResponse(summaryOpt.get());
        } else {
            // Yeni kullanıcıysa, boş/sıfırlı bir DashboardResponse (DTO) oluştur ve gönder
            log.info("Kullanıcı (ID:{}) için henüz işlem yok, varsayılan dashboard döndürülüyor.", currentUser.getId());
            return DashboardResponse.builder()
                    .userId(currentUser.getId())
                    .fullName(currentUser.getFullName())
                    .totalIncome(java.math.BigDecimal.ZERO)
                    .totalExpense(java.math.BigDecimal.ZERO)
                    .netBalance(java.math.BigDecimal.ZERO)
                    .currentMonthExpenseTotal(java.math.BigDecimal.ZERO)
                    .potentialMonthlySavings(java.math.BigDecimal.ZERO)
                    .monthlySavingsGoal(currentUser.getMonthlySavingsGoal())
                    .declaredMonthlyIncome(currentUser.getMonthlyIncome())
                    .savingsGoalProgressPct(0.0)
                    .smartSpendingScore(0.0)
                    .build();
        }
    }

    // ─── Private Yardımcılar ─────────────────────────────────────────────────

    /**
     * SecurityContextHolder'dan oturum açmış kullanıcıyı alır.
     * User entity'miz UserDetails implement ettiğinden doğrudan cast yapılabilir.
     */

    /**
     * DashboardSummary entity → DashboardResponse DTO dönüşümü.
     * Entity'nin tüm alanları DTO'ya bire bir taşınır.
     */
    private DashboardResponse mapToResponse(DashboardSummary s) {
        // Negatif değerleri clamp et
        Double savingsProgress = s.getSavingsGoalProgressPct();
        if (savingsProgress == null || savingsProgress.isNaN() || savingsProgress.isInfinite()) {
            savingsProgress = 0.0;
        }
        savingsProgress = Math.max(0.0, Math.min(100.0, savingsProgress));

        Double spendingScore = s.getSmartSpendingScore();
        if (spendingScore == null || spendingScore.isNaN() || spendingScore.isInfinite()) {
            spendingScore = 0.0;
        }
        spendingScore = Math.max(0.0, Math.min(100.0, spendingScore));

        return DashboardResponse.builder()
                .userId(s.getUserId())
                .fullName(s.getFullName())
                .totalIncome(s.getTotalIncome())
                .totalExpense(s.getTotalExpense())
                .netBalance(s.getNetBalance())
                .currentMonthExpenseTotal(s.getCurrentMonthExpenseTotal())
                .potentialMonthlySavings(s.getPotentialMonthlySavings())
                .monthlySavingsGoal(s.getMonthlySavingsGoal())
                .declaredMonthlyIncome(s.getDeclaredMonthlyIncome())
                .savingsGoalProgressPct(savingsProgress)
                .smartSpendingScore(spendingScore)
                .build();
    }
}