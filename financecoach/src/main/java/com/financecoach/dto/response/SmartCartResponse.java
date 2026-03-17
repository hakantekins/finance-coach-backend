package com.financecoach.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Akıllı Sepet — market bazlı dağıtım sonucu.
 *
 * Kullanıcı sepetindeki ürünleri en ucuz marketlere dağıtır.
 * Toplam tasarrufu ikinci en ucuz toplam üzerinden hesaplar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartCartResponse {

    /** Sepetteki tüm ürünler (fiyat bilgileriyle) */
    private List<CartItemResponse> items;

    /**
     * Market bazlı dağıtım.
     * Key: Market adı ("BİM", "A101", ...)
     * Value: O marketten alınacak ürün listesi
     */
    private Map<String, List<MarketGroup>> marketDistribution;

    /** En ucuz kombinasyonla toplam maliyet */
    private BigDecimal optimizedTotal;

    /** İkinci en ucuz marketten alınsa toplam ne tutardı */
    private BigDecimal secondBestTotal;

    /** Tasarruf = secondBestTotal - optimizedTotal */
    private BigDecimal totalSavings;

    /** Kaç farklı markete gidilmesi gerekiyor */
    private int marketCount;

    /** Sepetteki toplam ürün sayısı */
    private int itemCount;

    /**
     * Bir marketten alınacak ürün grubu
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketGroup {
        private String productName;
        private String unit;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalCost;
    }
}