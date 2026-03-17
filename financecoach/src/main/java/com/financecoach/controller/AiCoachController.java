package com.financecoach.controller;

import com.financecoach.dto.response.AiRecommendationResponse;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.service.AiCoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Finans Koçu REST Controller.
 * Base URL: /api/v1/coach
 *
 * DÜZELTME: /advice endpoint'i artık düz String yerine
 * Map<String, String> döndürüyor.
 *
 * Neden: SavingsCoach.tsx frontend'i şunu yapıyor:
 *   coachResponse.data.data?.message
 *
 * Eski halde backend'den gelen yapı:
 *   { success: true, data: "tavsiye metni" }  ← data bir String
 *
 * Frontend data.message arıyor ama data bir String, dolayısıyla
 * data.message = undefined → UI'da boş görünüyor.
 *
 * Yeni yapı:
 *   { success: true, data: { message: "tavsiye metni" } }
 *
 * Artık frontend doğru şekilde erişebilir.
 */
@RestController
@RequestMapping("/v1/coach")
@RequiredArgsConstructor
@Slf4j
public class AiCoachController {

    private final AiCoachService aiCoachService;

    /**
     * GET /api/v1/coach/advice
     *
     * DÜZELTME: String yerine Map<String, String> döndürülüyor.
     * Frontend: coachResponse.data.data.message ile erişir.
     */
    @GetMapping("/advice")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFinancialAdvice() {
        log.debug("GET /v1/coach/advice isteği alındı");
        String advice = aiCoachService.getFinancialAdvice();
        // Frontend'in beklediği yapı: { data: { message: "..." } }
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("message", advice), "Finansal tavsiyeniz hazır")
        );
    }

    /**
     * GET /api/v1/coach/analyze/category
     */
    @GetMapping("/analyze/category")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> analyzeTopCategory() {
        log.debug("GET /v1/coach/analyze/category isteği alındı");
        AiRecommendationResponse result = aiCoachService.analyzeTopCategory();
        return ResponseEntity.ok(
                ApiResponse.success(result, "'" + result.getAnalyzedCategory() + "' kategorisi analiz edildi")
        );
    }

    /**
     * GET /api/v1/coach/analyze/market?product=Tam Yağlı Süt
     */
    @GetMapping("/analyze/market")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> compareMarketPrices(
            @RequestParam String product
    ) {
        log.debug("GET /v1/coach/analyze/market isteği alındı: product='{}'", product);
        AiRecommendationResponse result = aiCoachService.compareMarketPrices(product);
        return ResponseEntity.ok(
                ApiResponse.success(result, "'" + product + "' için market karşılaştırması tamamlandı")
        );
    }

    /**
     * GET /api/v1/coach/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AiRecommendationResponse>>> getRecommendationHistory() {
        log.debug("GET /v1/coach/history isteği alındı");
        List<AiRecommendationResponse> history = aiCoachService.getRecommendationHistory();
        return ResponseEntity.ok(
                ApiResponse.success(history, history.size() + " tavsiye geçmişi listelendi")
        );
    }
}