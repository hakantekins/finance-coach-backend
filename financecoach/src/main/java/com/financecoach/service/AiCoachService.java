package com.financecoach.service;

import com.financecoach.dto.response.AiRecommendationResponse;

import java.util.List;

/**
 * AI Finans Koçu servis arayüzü — genişletilmiş versiyon.
 * <p>
 * Üç farklı analiz modu:
 * 1. Genel tavsiye           : Tüm gelir/gider profilini analiz eder
 * 2. Kategori uyarısı        : En çok harcama yapılan kategoriyi odaklar
 * 3. Market karşılaştırması  : MarketPrice tablosuyla fiyat optimizasyonu
 * <p>
 * Her mod ürettiği tavsiyeyi veritabanına kaydeder (AiRecommendation entity).
 */
public interface AiCoachService {

    /**
     * Genel gelir/gider profiline dayalı finansal tavsiye üretir ve saklar.
     * Mevcut GET /advice endpoint'iyle geriye dönük uyumludur.
     *
     * @return Üretilen tavsiye metni
     */
    String getFinancialAdvice();

    /**
     * En çok harcama yapılan kategoriyi tespit edip
     * bu kategoriye özel tavsiye üretir.
     * CATEGORY_ALERT tipiyle veritabanına kaydedilir.
     *
     * @return Kaydedilen tavsiye DTO'su
     */
    AiRecommendationResponse analyzeTopCategory();

    /**
     * Belirli bir ürünün marketler arası fiyatını karşılaştırır,
     * en ucuz alternatifi bulur, tasarruf yüzdesini hesaplar.
     * MARKET_COMPARISON tipiyle veritabanına kaydedilir.
     *
     * @param productName MarketPrice.productName ile eşleşen ürün adı
     * @return Kaydedilen tavsiye DTO'su
     */
    AiRecommendationResponse compareMarketPrices(String productName);

    /**
     * Kullanıcının geçmiş tavsiye geçmişini döner.
     *
     * @return Tarihe göre azalan sıralı tavsiye listesi
     */
    List<AiRecommendationResponse> getRecommendationHistory();
}