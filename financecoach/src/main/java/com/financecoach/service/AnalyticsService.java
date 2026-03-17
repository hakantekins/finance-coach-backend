package com.financecoach.service;

import com.financecoach.dto.response.MonthlySavingsResponse;

import java.util.List;

/**
 * Analitik raporlama servis ARAYÜZÜ (interface).
 *
 * IntelliJ'de açmak için:
 *   src/main/java/com/financecoach/service/AnalyticsService.java
 *   → New → Java Class → Kind: Interface seç
 *   ya da bu dosyayı direkt kopyala.
 *
 * Implementasyonu: AnalyticsServiceImpl (service/impl paketi altında)
 */
public interface AnalyticsService {

    /**
     * Kullanıcının son 12 aylık kümülatif birikim gelişimini döner.
     *
     * Hesaplama mantığı:
     *   Ocak   savings = Oca gelir - Oca gider
     *   Şubat  savings = Ocak savings + (Şub gelir - Şub gider)
     *   Mart   savings = Şubat savings + (Mar gelir - Mar gider)
     *   ...
     *
     * Veri olmayan aylar sıfır gelir/gider ile hesaplanır
     * (bir önceki ayın birikimi korunur, sıfıra düşmez).
     *
     * Kullanıcı kimliği SecurityContextHolder'dan alınır,
     * metoda parametre geçilmez.
     *
     * @return Son 12 aya ait aylık birikim listesi, kronolojik sırada (Oca → Ara)
     */
    List<MonthlySavingsResponse> getMonthlySavings();
}