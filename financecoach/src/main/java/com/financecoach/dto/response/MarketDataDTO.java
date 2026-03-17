package com.financecoach.dto.response;

import java.math.BigDecimal;

public record MarketDataDTO(
        String instrumentName, // Örn: Altın (Gram), BIST 100
        String symbol,         // Örn: XAU, XU100
        BigDecimal price,      // Güncel Fiyat
        Double changePercent,  // Günlük Değişim %
        String trend           // "UP" veya "DOWN"
) {}