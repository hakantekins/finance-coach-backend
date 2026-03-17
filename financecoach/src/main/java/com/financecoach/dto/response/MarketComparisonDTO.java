package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Frontend tarafındaki karşılaştırma tablosu için optimize edilmiş veri yapısı.
 * Bir ürünün tüm marketlerdeki fiyatlarını ve tasarruf miktarını taşır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketComparisonDTO {

    private String productName;
    private String category; // Filtreleme için lazım
    private String unit;     // "Adet", "Kg", "Litre" bilgisi için

    /**
     * Key   : Market adı (BİM, Şok, A101, Migros)
     * Value : O marketteki fiyat
     */
    private Map<String, BigDecimal> marketPrices;

    private String cheapestMarket;
    private BigDecimal cheapestPrice;

    /**
     * En pahalı fiyat ile en ucuz fiyat arasındaki fark.
     * Kullanıcıya "Şu kadar tasarruf edebilirsin" demek için kullanılır.
     */
    private BigDecimal priceDifference;
}