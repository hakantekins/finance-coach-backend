package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPriceResponse {
    private Long id;
    private String productName;
    private String category;
    private String marketName;
    private BigDecimal price;
    private BigDecimal previousPrice;
    private LocalDate priceDate;
    private String unit;
    private String productUrl;
    private String imageUrl;
    private boolean active;
    private OffsetDateTime createdAt;
}