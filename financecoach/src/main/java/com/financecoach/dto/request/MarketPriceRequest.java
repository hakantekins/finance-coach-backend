package com.financecoach.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POST /api/v1/market-prices için istek gövdesi.
 * Tüm alanlar Bean Validation ile doğrulanır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPriceRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(min = 2, max = 200, message = "Ürün adı 2-200 karakter arasında olmalıdır")
    private String productName;

    @NotBlank(message = "Market adı boş olamaz")
    @Size(min = 2, max = 100, message = "Market adı 2-100 karakter arasında olmalıdır")
    private String marketName;

    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalıdır")
    @Digits(integer = 8, fraction = 2, message = "Fiyat en fazla 8 tam, 2 ondalık basamak içerebilir")
    private BigDecimal price;

    /**
     * Null gelirse Service katmanında bugünün tarihi atanır.
     * Gelecek tarihli fiyat girişine izin verilmez.
     */
    @PastOrPresent(message = "Fiyat tarihi gelecekte olamaz")
    private LocalDate priceDate;

    @Size(max = 50, message = "Birim en fazla 50 karakter olabilir")
    private String unit;
}