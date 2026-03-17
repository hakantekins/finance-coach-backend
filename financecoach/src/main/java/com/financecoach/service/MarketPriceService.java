package com.financecoach.service;

import com.financecoach.dto.request.MarketPriceRequest;
import com.financecoach.dto.response.MarketComparisonDTO;
import com.financecoach.dto.response.MarketPriceResponse;

import java.util.List;

/**
 * Market fiyatı iş mantığı için servis arayüzü.
 */
public interface MarketPriceService {

    /** Yeni market fiyatı kaydı ekler. */
    MarketPriceResponse addPrice(MarketPriceRequest request);

    /** Tüm fiyat kayıtlarını döner. */
    List<MarketPriceResponse> getAllPrices();

    /** Ürün adına göre kısmi eşleşmeli arama yapar. */
    List<MarketPriceResponse> searchByProductName(String productName);

    /** Market adına göre filtreleme yapar. */
    List<MarketPriceResponse> filterByMarketName(String marketName);

    /** Belirli bir ürünün tüm marketlerdeki fiyatlarını List<MarketPriceResponse> olarak döner. */
    List<MarketPriceResponse> compareByProductName(String productName);

    /**
     * Ham List<MarketPriceResponse> listesini ürün bazında gruplayıp
     * Dashboard tablosuna uygun List<MarketComparisonDTO> yapısına dönüştürür.
     * * @param prices Ham fiyat listesi
     * @return Ürün bazında gruplanmış karşılaştırma listesi
     */
    List<MarketComparisonDTO> buildComparisonList(List<MarketPriceResponse> prices);

    /** Tüm fiyatları kategori → ürün bazlı gruplanmış döner */
    List<MarketPriceResponse> getAllPricesGrouped();

    /** Benzersiz kategori listesi */
    List<String> getCategories();
}