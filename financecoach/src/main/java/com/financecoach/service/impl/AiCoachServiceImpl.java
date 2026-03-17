package com.financecoach.service.impl;

import com.financecoach.dto.response.AiRecommendationResponse;
import com.financecoach.dto.response.MarketPriceResponse;
import com.financecoach.dto.response.TransactionResponse;
import com.financecoach.model.entity.AiRecommendation;
import com.financecoach.model.entity.User;
import com.financecoach.repository.AiRecommendationRepository;
import com.financecoach.service.AiCoachService;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.MarketPriceService;
import com.financecoach.service.TransactionService;
import com.financecoach.service.ai.PromptGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiCoachServiceImpl extends BaseAuthService implements AiCoachService {

    private final TransactionService         transactionService;
    private final MarketPriceService         marketPriceService;
    private final AiRecommendationRepository recommendationRepository;
    private final PromptGenerator            promptGenerator;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Türkçe zorlama system prompt'u.
     * - Rol tanımı net: Türk finans danışmanı
     * - Dil kısıtlaması tekrarlı ve güçlü
     * - Format kısıtlaması: kısa, somut, giriş cümlesi yok
     * - Negatif talimat: İngilizce/Çince kesinlikle yasak
     */
    private static final String SYSTEM_PROMPT = """
            Sen Türkiye'de yaşayan deneyimli bir kişisel finans danışmanısın. \
            Adın "Finans Koçu".

            KESİN KURALLAR:
            1. YALNIZCA TÜRKÇE yaz. Tek bir İngilizce, Çince veya başka dilde kelime bile kullanma.
            2. En fazla 3 kısa cümle yaz. Uzun paragraflar yazma.
            3. Somut rakam ve TL cinsinden tasarruf tutarı hesapla.
            4. Doğrudan öneriye başla — "Merhaba", "Tabii ki" gibi giriş cümlesi yazma.
            5. Madde işareti veya numara listesi kullanma, düz cümle yaz.
            6. "recommend", "suggest", "savings" gibi İngilizce kelimeler kesinlikle YASAK.
            7. Samimi ve dostane bir Türkçe kullan, "siz" hitabı ile yaz.""";

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.max-transactions-in-prompt:10}")
    private int maxTransactionsInPrompt;

    @Value("${app.ai.simulation-mode:false}")
    private boolean simulationMode;

    // =========================================================================
    // 1. GENEL FİNANSAL TAVSİYE
    // =========================================================================

    @Override
    @Transactional
    public String getFinancialAdvice() {
        List<TransactionResponse> transactions = transactionService.getUserTransactions();
        log.debug("Genel AI tavsiyesi üretiliyor: {} işlem", transactions.size());

        String prompt = promptGenerator.buildGeneralAdvicePrompt(transactions, maxTransactionsInPrompt);
        String advice = resolveAdvice(prompt);

        saveRecommendation(advice, prompt, AiRecommendation.RecommendationType.GENERAL, null);
        log.info("Genel AI tavsiyesi üretildi ve kaydedildi");
        return advice;
    }

    // =========================================================================
    // 2. KATEGORİ UYARISI ANALİZİ
    // =========================================================================

    @Override
    @Transactional
    public AiRecommendationResponse analyzeTopCategory() {
        List<TransactionResponse> transactions = transactionService.getUserTransactions();
        String topCategory = promptGenerator.findTopCategory(transactions);

        log.debug("Kategori analizi: en yüksek kategori='{}'", topCategory);

        String prompt = promptGenerator.buildCategoryAlertPrompt(transactions);
        String advice = resolveAdvice(prompt);

        AiRecommendation saved = saveRecommendation(
                advice, prompt,
                AiRecommendation.RecommendationType.CATEGORY_ALERT,
                topCategory
        );

        log.info("Kategori uyarısı kaydedildi: kategori='{}', id={}", topCategory, saved.getId());
        return mapToResponse(saved);
    }

    // =========================================================================
    // 3. MARKET KARŞILAŞTIRMASI
    // =========================================================================

    @Override
    @Transactional
    public AiRecommendationResponse compareMarketPrices(String productName) {
        List<TransactionResponse> transactions = transactionService.getUserTransactions();
        List<MarketPriceResponse> marketPrices = marketPriceService.compareByProductName(productName);

        log.debug("Market karşılaştırması: ürün='{}', {} market bulundu",
                productName, marketPrices.size());

        String prompt = promptGenerator.buildMarketComparisonPrompt(
                transactions, marketPrices, productName
        );
        String advice = resolveAdvice(prompt);

        AiRecommendation saved = saveRecommendation(
                advice, prompt,
                AiRecommendation.RecommendationType.MARKET_COMPARISON,
                productName
        );

        log.info("Market karşılaştırma tavsiyesi kaydedildi: ürün='{}', id={}",
                productName, saved.getId());
        return mapToResponse(saved);
    }

    // =========================================================================
    // 4. TAVSİYE GEÇMİŞİ
    // =========================================================================

    @Override
    public List<AiRecommendationResponse> getRecommendationHistory() {
        User currentUser = getAuthenticatedUser();
        log.debug("Tavsiye geçmişi çekiliyor: userId={}", currentUser.getId());

        return recommendationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // PRIVATE YARDIMCI METODLAR
    // =========================================================================

    private String resolveAdvice(String prompt) {
        if (simulationMode) {
            log.info("Simülasyon modu aktif - LLM API çağrısı atlanıyor");
            return promptGenerator.simulateAdvice(prompt);
        }
        return callLlmApi(prompt);
    }

    private AiRecommendation saveRecommendation(
            String advice,
            String prompt,
            AiRecommendation.RecommendationType type,
            String analyzedCategory
    ) {
        User currentUser = getAuthenticatedUser();

        AiRecommendation recommendation = AiRecommendation.builder()
                .userId(currentUser.getId())
                .type(type)
                .message(advice)
                .promptUsed(prompt)
                .analyzedCategory(analyzedCategory)
                .build();

        return recommendationRepository.save(recommendation);
    }

    /**
     * Groq Chat Completions API çağrısı.
     * - Static ObjectMapper ve HttpClient (performans)
     * - temperature=0.3 (daha tutarlı, daha az hallucination)
     * - max_tokens=300 (kısa ve öz cevap zorla)
     * - System prompt ile güçlü Türkçe zorlama
     */
    private String callLlmApi(String userPrompt) {
        try {
            // User prompt'u JSON-safe hale getir
            String escapedUserPrompt = MAPPER.writeValueAsString(userPrompt);
            String escapedSystemPrompt = MAPPER.writeValueAsString(SYSTEM_PROMPT);

            String requestBody = """
                {
                    "model": "%s",
                    "temperature": 0.3,
                    "max_tokens": 300,
                    "messages": [
                        {
                            "role": "system",
                            "content": %s
                        },
                        {
                            "role": "user",
                            "content": %s
                        }
                    ]
                }
                """.formatted(model, escapedSystemPrompt, escapedUserPrompt);

            String apiUrl = baseUrl.endsWith("/")
                    ? baseUrl + "chat/completions"
                    : baseUrl + "/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.debug("Groq API status: {}, body length: {}",
                    response.statusCode(), response.body().length());

            if (response.statusCode() != 200) {
                log.error("Groq API hata: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Groq API hata kodu: " + response.statusCode());
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode choices = root.get("choices");

            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText().trim();
                    log.info("Groq AI tavsiye üretildi: {} karakter", content.length());
                    return content;
                }
            }

            throw new RuntimeException("Groq API beklenmeyen yanıt formatı");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM API hatası: {}", e.getMessage());
            throw new RuntimeException("Yapay zeka servisiyle iletişim kurulamadı: " + e.getMessage(), e);
        }
    }

    private AiRecommendationResponse mapToResponse(AiRecommendation rec) {
        return AiRecommendationResponse.builder()
                .id(rec.getId())
                .userId(rec.getUserId())
                .type(rec.getType())
                .message(rec.getMessage())
                .analyzedCategory(rec.getAnalyzedCategory())
                .createdAt(rec.getCreatedAt())
                .build();
    }
}