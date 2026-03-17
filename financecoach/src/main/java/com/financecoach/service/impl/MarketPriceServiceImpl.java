package com.financecoach.service.impl;

import com.financecoach.dto.request.MarketPriceRequest;
import com.financecoach.dto.response.MarketComparisonDTO;
import com.financecoach.dto.response.MarketPriceResponse;
import com.financecoach.model.entity.MarketPrice;
import com.financecoach.repository.MarketPriceRepository;
import com.financecoach.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MarketPriceServiceImpl implements MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MarketPriceResponse addPrice(MarketPriceRequest request) {
        log.debug("Yeni fiyat ekleniyor: urun={}, market={}, fiyat={}",
                request.getProductName(), request.getMarketName(), request.getPrice());

        MarketPrice marketPrice = MarketPrice.builder()
                .productName(request.getProductName())
                .marketName(request.getMarketName())
                .price(request.getPrice())
                .priceDate(request.getPriceDate() != null ? request.getPriceDate() : LocalDate.now())
                .unit(request.getUnit())
                .build();

        MarketPrice saved = marketPriceRepository.save(marketPrice);
        log.info("Fiyat kaydedildi: id={}, urun={}, market={}",
                saved.getId(), saved.getProductName(), saved.getMarketName());

        return mapToResponse(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Override
    public List<MarketPriceResponse> getAllPrices() {
        return marketPriceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketPriceResponse> searchByProductName(String productName) {
        return marketPriceRepository
                .findByProductNameContainingIgnoreCaseOrderByPriceDateDesc(productName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketPriceResponse> filterByMarketName(String marketName) {
        return marketPriceRepository
                .findByMarketNameIgnoreCaseOrderByPriceDateDesc(marketName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketPriceResponse> compareByProductName(String productName) {
        return marketPriceRepository
                .findLatestPricesByProductName(productName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── KARŞILAŞTIRMA DÖNÜŞÜMÜ ──────────────────────────────────────────────

    @Override
    public List<MarketComparisonDTO> buildComparisonList(List<MarketPriceResponse> prices) {

        if (prices == null || prices.isEmpty()) {
            log.debug("buildComparisonList: bos liste alindi, bos liste donuluyor");
            return Collections.emptyList();
        }

        return prices.stream()
                .filter(p -> p.getProductName() != null && p.getPrice() != null)
                .collect(Collectors.groupingBy(MarketPriceResponse::getProductName))
                .entrySet().stream()
                .map(entry -> {
                    String productName = entry.getKey();
                    List<MarketPriceResponse> group = entry.getValue();

                    Map<String, BigDecimal> marketPrices = group.stream()
                            .collect(Collectors.toMap(
                                    p -> Optional.ofNullable(p.getMarketName())
                                            .filter(m -> !m.isBlank())
                                            .orElse("Bilinmeyen"),
                                    MarketPriceResponse::getPrice,
                                    BigDecimal::min,
                                    TreeMap::new
                            ));

                    Map.Entry<String, BigDecimal> cheapestEntry = marketPrices.entrySet()
                            .stream()
                            .min(Map.Entry.comparingByValue())
                            .orElse(null);

                    String cheapestMarket = cheapestEntry != null
                            ? cheapestEntry.getKey() : null;
                    BigDecimal cheapestPrice = cheapestEntry != null
                            ? cheapestEntry.getValue() : BigDecimal.ZERO;

                    BigDecimal secondCheapestPrice = marketPrices.values().stream()
                            .sorted()
                            .skip(1)
                            .findFirst()
                            .orElse(cheapestPrice);

                    BigDecimal priceDifference = secondCheapestPrice.subtract(cheapestPrice);

                    // Gruptan kategori ve unit bilgisini al
                    String category = group.stream()
                            .map(MarketPriceResponse::getCategory)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);

                    String unit = group.stream()
                            .map(MarketPriceResponse::getUnit)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);

                    return MarketComparisonDTO.builder()
                            .productName(productName)
                            .category(category)
                            .unit(unit)
                            .marketPrices(marketPrices)
                            .cheapestMarket(cheapestMarket)
                            .cheapestPrice(cheapestPrice)
                            .priceDifference(priceDifference)
                            .build();
                })
                .sorted(Comparator
                        .comparing(MarketComparisonDTO::getCategory,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MarketComparisonDTO::getProductName))
                .collect(Collectors.toList());
    }

    // ─── Private Mapping ─────────────────────────────────────────────────────
    @Override
    public List<MarketPriceResponse> getAllPricesGrouped() {
        return marketPriceRepository.findAllGroupedByCategoryAndProduct()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getCategories() {
        return marketPriceRepository.findDistinctCategories();
    }
    private MarketPriceResponse mapToResponse(MarketPrice mp) {
        return MarketPriceResponse.builder()
                .id(mp.getId())
                .productName(mp.getProductName())
                .marketName(mp.getMarketName())
                .category(mp.getCategory())
                .price(mp.getPrice())
                .previousPrice(mp.getPreviousPrice())
                .priceDate(mp.getPriceDate())
                .unit(mp.getUnit())
                .productUrl(mp.getProductUrl())
                .imageUrl(mp.getImageUrl())
                .active(mp.isActive())
                .createdAt(mp.getCreatedAt())
                .build();
    }
}