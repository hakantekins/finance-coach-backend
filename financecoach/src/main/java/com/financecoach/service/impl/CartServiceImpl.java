package com.financecoach.service.impl;

import com.financecoach.dto.request.AddToCartRequest;
import com.financecoach.dto.response.CartItemResponse;
import com.financecoach.dto.response.SmartCartResponse;
import com.financecoach.exception.ResourceNotFoundException;
import com.financecoach.model.entity.CartItem;
import com.financecoach.model.entity.MarketPrice;
import com.financecoach.model.entity.User;
import com.financecoach.repository.CartItemRepository;
import com.financecoach.repository.MarketPriceRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.CartService;
import com.financecoach.util.ProductNameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartServiceImpl extends BaseAuthService implements CartService {

    private final CartItemRepository cartItemRepository;
    private final MarketPriceRepository marketPriceRepository;

    // ─── SEPETE EKLE ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartItemResponse addToCart(AddToCartRequest request) {
        User user = getAuthenticatedUser();
        String productName = request.getProductName().trim();

        // Aynı ürün zaten sepette varsa miktarı artır
        Optional<CartItem> existing = cartItemRepository
                .findByUserIdAndProductNameAndCompletedFalse(user.getId(), productName);

        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            log.debug("Sepetteki ürün güncellendi: '{}' x{}", productName, item.getQuantity());
        } else {
            item = CartItem.builder()
                    .userId(user.getId())
                    .productName(productName)
                    .quantity(request.getQuantity())
                    .build();
            log.debug("Sepete yeni ürün eklendi: '{}'", productName);
        }

        CartItem saved = cartItemRepository.save(item);
        return buildCartItemResponse(saved);
    }

    // ─── SEPETİ GETİR ────────────────────────────────────────────────────────

    @Override
    public List<CartItemResponse> getCartItems() {
        User user = getAuthenticatedUser();
        List<CartItem> items = cartItemRepository
                .findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId());

        return items.stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());
    }

    // ─── AKILLI SEPET — MARKET BAZLI DAĞITIM ─────────────────────────────────

    @Override
    public SmartCartResponse getSmartCart() {
        User user = getAuthenticatedUser();
        List<CartItem> items = cartItemRepository
                .findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId());

        if (items.isEmpty()) {
            return SmartCartResponse.builder()
                    .items(List.of())
                    .marketDistribution(Map.of())
                    .optimizedTotal(BigDecimal.ZERO)
                    .secondBestTotal(BigDecimal.ZERO)
                    .totalSavings(BigDecimal.ZERO)
                    .marketCount(0)
                    .itemCount(0)
                    .build();
        }

        // 1. Her ürün için fiyat bilgilerini topla
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());

        // 2. Market bazlı dağıtım: her ürünü en ucuz markete ata
        Map<String, List<SmartCartResponse.MarketGroup>> distribution = new LinkedHashMap<>();
        BigDecimal optimizedTotal = BigDecimal.ZERO;

        // İkinci en ucuz toplam hesabı için
        Map<String, BigDecimal> secondBestPricePerItem = new HashMap<>();

        for (CartItemResponse item : itemResponses) {
            if (item.getCheapestMarket() == null || item.getCheapestPrice() == null) {
                continue;
            }

            String market = item.getCheapestMarket();
            BigDecimal totalCost = item.getCheapestPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            SmartCartResponse.MarketGroup group = SmartCartResponse.MarketGroup.builder()
                    .productName(item.getProductName())
                    .unit(item.getUnit())
                    .price(item.getCheapestPrice())
                    .quantity(item.getQuantity())
                    .totalCost(totalCost)
                    .build();

            distribution.computeIfAbsent(market, k -> new ArrayList<>()).add(group);
            optimizedTotal = optimizedTotal.add(totalCost);

            BigDecimal secondBest = findSecondCheapestPrice(item.getMarketPrices());
            if (secondBest != null) {
                secondBestPricePerItem.put(item.getProductName(),
                        secondBest.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal secondBestTotal = secondBestPricePerItem.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (secondBestTotal.compareTo(BigDecimal.ZERO) == 0) {
            secondBestTotal = optimizedTotal;
        }

        BigDecimal totalSavings = secondBestTotal.subtract(optimizedTotal);

        log.info("Akıllı sepet hesaplandı: {} ürün, {} market, tasarruf={}",
                itemResponses.size(), distribution.size(), totalSavings);

        return SmartCartResponse.builder()
                .items(itemResponses)
                .marketDistribution(distribution)
                .optimizedTotal(optimizedTotal)
                .secondBestTotal(secondBestTotal)
                .totalSavings(totalSavings)
                .marketCount(distribution.size())
                .itemCount(itemResponses.size())
                .build();
    }

    // ─── SEPETTEN SİL ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void removeFromCart(Long itemId) {
        User user = getAuthenticatedUser();
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sepet öğesi bulunamadı: id=" + itemId));
        cartItemRepository.delete(item);
        log.debug("Sepetten silindi: '{}'", item.getProductName());
    }

    // ─── MİKTAR GÜNCELLE ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartItemResponse updateQuantity(Long itemId, int quantity) {
        User user = getAuthenticatedUser();
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sepet öğesi bulunamadı: id=" + itemId));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        item.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(item);
        return buildCartItemResponse(saved);
    }

    // ─── SEPETİ TEMİZLE ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart() {
        User user = getAuthenticatedUser();
        cartItemRepository.deleteByUserIdAndCompletedFalse(user.getId());
        log.info("Sepet temizlendi: userId={}", user.getId());
    }

    // ─── ALIŞVERİŞİ TAMAMLA ─────────────────────────────────────────────────

    @Override
    @Transactional
    public void completeCart() {
        User user = getAuthenticatedUser();
        List<CartItem> items = cartItemRepository
                .findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId());

        if (items.isEmpty()) {
            throw new RuntimeException("Sepetiniz boş, tamamlanacak alışveriş yok.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (CartItem item : items) {
            item.setCompleted(true);
            item.setCompletedAt(now);
        }
        cartItemRepository.saveAll(items);
        log.info("Alışveriş tamamlandı: userId={}, {} ürün", user.getId(), items.size());
    }

    // ─── SEPET SAYISI ────────────────────────────────────────────────────────

    @Override
    public long getCartCount() {
        User user = getAuthenticatedUser();
        return cartItemRepository.countByUserIdAndCompletedFalse(user.getId());
    }

    // ─── PRIVATE YARDIMCILAR ─────────────────────────────────────────────────

    /**
     * CartItem'ı CartItemResponse'a dönüştürür.
     * MarketPrice tablosundan ürün fiyatlarını çeker.
     *
     * Eşleştirme stratejisi (öncelik sırasıyla):
     *   1. Normalize edilmiş EXACT MATCH (birim + boşluk farkları tolere edilir)
     *   2. DB LIKE match (partial, eski davranış — fallback)
     */
    private CartItemResponse buildCartItemResponse(CartItem item) {
        String normalizedInput = ProductNameNormalizer.normalize(item.getProductName());

        // 1. DB'den partial match ile aday ürünleri çek
        List<MarketPrice> candidates = marketPriceRepository
                .findByProductNameContainingIgnoreCaseOrderByPriceDateDesc(item.getProductName());

        // 2. Normalize exact match: her iki tarafı da normalize edip karşılaştır
        List<MarketPrice> normalizedExact = candidates.stream()
                .filter(mp -> ProductNameNormalizer.normalize(mp.getProductName())
                        .equals(normalizedInput))
                .toList();

        // 3. Eski exact match (birebir, case-insensitive) — fallback
        List<MarketPrice> caseExact = candidates.stream()
                .filter(mp -> mp.getProductName().equalsIgnoreCase(item.getProductName()))
                .toList();

        // Öncelik: normalizedExact > caseExact > tüm candidates
        List<MarketPrice> selectedPrices;
        if (!normalizedExact.isEmpty()) {
            selectedPrices = normalizedExact;
            log.trace("Normalized exact match: '{}' → {} sonuç", item.getProductName(), normalizedExact.size());
        } else if (!caseExact.isEmpty()) {
            selectedPrices = caseExact;
            log.trace("Case exact match: '{}' → {} sonuç", item.getProductName(), caseExact.size());
        } else {
            selectedPrices = candidates;
            log.trace("Partial match fallback: '{}' → {} sonuç", item.getProductName(), candidates.size());
        }

        // Market → Fiyat map'i (her market için en güncel fiyat)
        Map<String, BigDecimal> marketPrices = new LinkedHashMap<>();
        String unit = null;
        String category = null;

        Map<String, MarketPrice> latestPerMarket = new LinkedHashMap<>();
        for (MarketPrice mp : selectedPrices) {
            String market = mp.getMarketName();
            if (!latestPerMarket.containsKey(market)) {
                latestPerMarket.put(market, mp);
            } else {
                if (mp.getPrice().compareTo(latestPerMarket.get(market).getPrice()) < 0) {
                    latestPerMarket.put(market, mp);
                }
            }
        }

        for (Map.Entry<String, MarketPrice> entry : latestPerMarket.entrySet()) {
            marketPrices.put(entry.getKey(), entry.getValue().getPrice());
            if (unit == null) unit = entry.getValue().getUnit();
            if (category == null) category = entry.getValue().getCategory();
        }

        // En ucuz marketi bul
        String cheapestMarket = null;
        BigDecimal cheapestPrice = null;

        for (Map.Entry<String, BigDecimal> entry : marketPrices.entrySet()) {
            if (cheapestPrice == null || entry.getValue().compareTo(cheapestPrice) < 0) {
                cheapestPrice = entry.getValue();
                cheapestMarket = entry.getKey();
            }
        }

        BigDecimal totalCost = (cheapestPrice != null)
                ? cheapestPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                : BigDecimal.ZERO;

        return CartItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .unit(unit)
                .category(category)
                .quantity(item.getQuantity())
                .marketPrices(marketPrices)
                .cheapestMarket(cheapestMarket)
                .cheapestPrice(cheapestPrice)
                .totalCost(totalCost)
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * Fiyat map'inden ikinci en ucuz fiyatı bulur.
     */
    private BigDecimal findSecondCheapestPrice(Map<String, BigDecimal> marketPrices) {
        if (marketPrices == null || marketPrices.size() < 2) return null;

        List<BigDecimal> sorted = marketPrices.values().stream()
                .sorted()
                .collect(Collectors.toList());

        return sorted.get(1);
    }
}