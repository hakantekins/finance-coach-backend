package com.financecoach.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;
    private String productName;
    private String unit;
    private String category;
    private Integer quantity;

    /** Her marketteki fiyat: { "BİM": 34.90, "A101": 36.75, ... } */
    private Map<String, BigDecimal> marketPrices;

    /** En ucuz market adı */
    private String cheapestMarket;

    /** En ucuz fiyat */
    private BigDecimal cheapestPrice;

    /** Toplam maliyet (cheapestPrice * quantity) */
    private BigDecimal totalCost;

    private OffsetDateTime createdAt;
}