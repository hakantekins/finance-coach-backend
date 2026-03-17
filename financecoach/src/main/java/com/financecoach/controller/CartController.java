package com.financecoach.controller;

import com.financecoach.dto.request.AddToCartRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.CartItemResponse;
import com.financecoach.dto.response.SmartCartResponse;
import com.financecoach.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Akıllı Sepet REST Controller
 * Base URL: /api/v1/cart
 *
 * POST   /api/v1/cart              → Sepete ürün ekle
 * GET    /api/v1/cart              → Sepeti getir
 * GET    /api/v1/cart/smart        → Akıllı dağıtım (market bazlı)
 * PUT    /api/v1/cart/{id}         → Miktar güncelle
 * DELETE /api/v1/cart/{id}         → Sepetten ürün sil
 * DELETE /api/v1/cart              → Sepeti temizle
 * POST   /api/v1/cart/complete     → Alışverişi tamamla
 * GET    /api/v1/cart/count        → Sepet sayısı
 * GET    /api/v1/cart/products     → Ürün arama (autocomplete)
 */
@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final com.financecoach.repository.MarketPriceRepository marketPriceRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request
    ) {
        log.debug("POST /v1/cart: ürün='{}'", request.getProductName());
        CartItemResponse item = cartService.addToCart(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Ürün sepete eklendi"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart() {
        List<CartItemResponse> items = cartService.getCartItems();
        return ResponseEntity.ok(
                ApiResponse.success(items, items.size() + " ürün sepette"));
    }

    @GetMapping("/smart")
    public ResponseEntity<ApiResponse<SmartCartResponse>> getSmartCart() {
        log.debug("GET /v1/cart/smart — akıllı dağıtım hesaplanıyor");
        SmartCartResponse smart = cartService.getSmartCart();
        return ResponseEntity.ok(
                ApiResponse.success(smart, "Akıllı sepet hazırlandı"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body
    ) {
        int quantity = body.getOrDefault("quantity", 1);
        log.debug("PUT /v1/cart/{}: miktar={}", id, quantity);
        CartItemResponse updated = cartService.updateQuantity(id, quantity);
        if (updated == null) {
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Ürün sepetten silindi (miktar 0)"));
        }
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Miktar güncellendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Ürün sepetten silindi"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(
                ApiResponse.success(null, "Sepet temizlendi"));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completeCart() {
        cartService.completeCart();
        return ResponseEntity.ok(
                ApiResponse.success(null, "Alışveriş tamamlandı"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCartCount() {
        long count = cartService.getCartCount();
        return ResponseEntity.ok(
                ApiResponse.success(count, "Sepet sayısı"));
    }

    /**
     * Ürün arama — sepete eklerken autocomplete için.
     * MarketPrice tablosundaki ürün adlarını döner.
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<String>>> searchProducts(
            @RequestParam String q
    ) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(
                    ApiResponse.success(List.of(), "En az 2 karakter girin"));
        }

        List<String> products = marketPriceRepository
                .findByProductNameContainingIgnoreCaseOrderByPriceDateDesc(q.trim())
                .stream()
                .map(com.financecoach.model.entity.MarketPrice::getProductName)
                .distinct()
                .limit(10)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(products, products.size() + " ürün bulundu"));
    }
}