package com.financecoach.controller;

import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.DashboardResponse;
import com.financecoach.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard (Özet Ekranı) REST Controller.
 * <p>
 * Base URL : /api/v1/dashboard
 * Auth     : JWT zorunlu (SecurityConfig → anyRequest().authenticated())
 * <p>
 * GET /api/v1/dashboard → Kullanıcının finansal özet ekranı
 */
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    // =========================================================================
    // GET /api/v1/dashboard
    // =========================================================================

    /**
     * Oturum açmış kullanıcının finansal dashboard özetini döner.
     * <p>
     * Dönen veriler:
     * - Tüm zamanların toplam geliri, gideri ve net bakiyesi
     * - Bu aya ait toplam harcama ve potansiyel tasarruf tutarı
     * - Tasarruf hedefi ilerleme yüzdesi (0-100)
     * - Akıllı Harcama Skoru (0-100)
     *
     * <pre>
     * GET /api/v1/dashboard
     * Authorization: Bearer {token}
     *
     * Yanıt:
     * {
     *   "success": true,
     *   "message": "Dashboard verisi başarıyla getirildi",
     *   "data": {
     *     "userId": "...",
     *     "fullName": "Ahmet Yılmaz",
     *     "totalIncome": 45000.00,
     *     "totalExpense": 32500.00,
     *     "netBalance": 12500.00,
     *     "currentMonthExpenseTotal": 4200.00,
     *     "potentialMonthlySavings": 850.00,
     *     "monthlySavingsGoal": 3000.00,
     *     "declaredMonthlyIncome": 15000.00,
     *     "savingsGoalProgressPct": 72.5,
     *     "smartSpendingScore": 79.8
     *   }
     * }
     * </pre>
     *
     * @return 200 OK + ApiResponse<DashboardResponse>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        log.debug("GET /v1/dashboard isteği alındı");

        DashboardResponse dashboard = dashboardService.getDashboard();

        return ResponseEntity.ok(
                ApiResponse.success(dashboard, "Dashboard verisi başarıyla getirildi")
        );
    }
}