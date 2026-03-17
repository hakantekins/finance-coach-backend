package com.financecoach.controller;

import java.util.Map;
import com.financecoach.dto.request.MarketPriceRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.MarketComparisonDTO;
import com.financecoach.dto.response.MarketPriceResponse;
import com.financecoach.service.MarketPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/market-prices")
@RequiredArgsConstructor
@Slf4j
public class MarketPriceController {

    private final MarketPriceService marketPriceService;
    private final List<com.financecoach.service.scraper.MarketScraper> scrapers;
    private final com.financecoach.repository.MarketPriceRepository marketPriceRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<MarketPriceResponse>> addPrice(
            @Valid @RequestBody MarketPriceRequest request
    ) {
        log.debug("POST /v1/market-prices: ürün={}, market={}", request.getProductName(), request.getMarketName());
        MarketPriceResponse created = marketPriceService.addPrice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Fiyat başarıyla eklendi"));
    }

    /**
     * GET /v1/market-prices
     * Sayfalama destekli — opsiyonel parametreler:
     *   ?page=0&size=50           → sayfalı sonuç
     *   ?category=Süt Ürünleri    → kategori filtreli (sayfalanabilir)
     *   ?grouped=true             → gruplu görünüm (sayfalama yok)
     *   parametresiz               → eski davranış (tüm liste)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getPrices(
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean grouped,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size
    ) {
        // Grouped mod — sayfalama uygulanmaz
        if (grouped) {
            List<MarketPriceResponse> prices = marketPriceService.getAllPricesGrouped();
            return ResponseEntity.ok(ApiResponse.success(prices, prices.size() + " kayıt listelendi"));
        }

        // Filtreleme modları — sayfalama uygulanmaz (genelde az sonuç döner)
        if (product != null && market != null) {
            List<MarketPriceResponse> prices = marketPriceService.searchByProductName(product).stream()
                    .filter(p -> p.getMarketName().equalsIgnoreCase(market)).toList();
            return ResponseEntity.ok(ApiResponse.success(prices, prices.size() + " kayıt listelendi"));
        }
        if (product != null) {
            List<MarketPriceResponse> prices = marketPriceService.searchByProductName(product);
            return ResponseEntity.ok(ApiResponse.success(prices, prices.size() + " kayıt listelendi"));
        }
        if (market != null) {
            List<MarketPriceResponse> prices = marketPriceService.filterByMarketName(market);
            return ResponseEntity.ok(ApiResponse.success(prices, prices.size() + " kayıt listelendi"));
        }

        // Sayfalama modu — page parametresi varsa
        if (page != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("category").ascending()
                    .and(Sort.by("productName").ascending()));

            Page<com.financecoach.model.entity.MarketPrice> result;
            if (category != null && !category.isBlank()) {
                result = marketPriceRepository.findByCategoryIgnoreCaseAndActiveTrue(category, pageable);
            } else {
                result = marketPriceRepository.findByActiveTrue(pageable);
            }

            // Entity → Response mapping
            List<MarketPriceResponse> content = result.getContent().stream()
                    .map(mp -> MarketPriceResponse.builder()
                            .id(mp.getId())
                            .productName(mp.getProductName())
                            .marketName(mp.getMarketName())
                            .price(mp.getPrice())
                            .priceDate(mp.getPriceDate())
                            .unit(mp.getUnit())
                            .category(mp.getCategory())
                            .build())
                    .toList();

            Map<String, Object> paged = Map.of(
                    "content", content,
                    "page", result.getNumber(),
                    "size", result.getSize(),
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages(),
                    "hasNext", result.hasNext(),
                    "hasPrevious", result.hasPrevious()
            );

            return ResponseEntity.ok(ApiResponse.success(paged,
                    result.getTotalElements() + " üründen " + content.size() + " tanesi listelendi"));
        }

        // Default — tüm liste (geriye uyumlu)
        List<MarketPriceResponse> prices = marketPriceService.getAllPrices();
        return ResponseEntity.ok(ApiResponse.success(prices, prices.size() + " kayıt listelendi"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = marketPriceService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, categories.size() + " kategori"));
    }

    @GetMapping("/dashboard-comparison")
    public ResponseEntity<ApiResponse<List<MarketComparisonDTO>>> getDashboardComparison() {
        log.debug("GET /v1/market-prices/dashboard-comparison");
        List<MarketPriceResponse> rawPrices = marketPriceService.getAllPrices();
        List<MarketComparisonDTO> tableData = marketPriceService.buildComparisonList(rawPrices);
        return ResponseEntity.ok(ApiResponse.success(tableData, "Market karşılaştırma tablosu hazırlandı"));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<List<MarketPriceResponse>>> comparePrices(
            @RequestParam String product
    ) {
        List<MarketPriceResponse> comparison = marketPriceService.compareByProductName(product);
        return ResponseEntity.ok(ApiResponse.success(comparison, product + " için fiyatlar karşılaştırıldı"));
    }

    @PostMapping("/scrape")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerScraping() {
        log.info("Manuel scraping tetiklendi — güvenli mod");

        Map<String, Object> results = new java.util.LinkedHashMap<>();
        int totalSaved = 0;

        for (com.financecoach.service.scraper.MarketScraper scraper : scrapers) {
            String marketName = scraper.getMarketName();
            try {
                List<com.financecoach.model.entity.MarketPrice> scraped = scraper.scrape();

                if (scraped.isEmpty()) {
                    results.put(marketName, Map.of(
                            "status", "BOŞ",
                            "count", 0,
                            "note", "Eski veriler korunuyor"
                    ));
                    continue;
                }

                List<com.financecoach.model.entity.MarketPrice> existing =
                        marketPriceRepository.findByMarketNameIgnoreCaseOrderByPriceDateDesc(marketName);
                if (!existing.isEmpty()) {
                    marketPriceRepository.deleteAll(existing);
                }

                marketPriceRepository.saveAll(scraped);
                totalSaved += scraped.size();

                results.put(marketName, Map.of(
                        "status", "OK",
                        "count", scraped.size(),
                        "sample", scraped.get(0).getProductName()
                ));

            } catch (Exception e) {
                results.put(marketName, Map.of(
                        "status", "HATA",
                        "error", e.getMessage(),
                        "note", "Eski veriler korunuyor"
                ));
                log.error("{} scraping hatası: {}", marketName, e.getMessage());
            }
        }

        results.put("toplam_kaydedilen", totalSaved);
        return ResponseEntity.ok(ApiResponse.success(results, "Scraping tamamlandı"));
    }
}