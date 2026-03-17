package com.financecoach.service;

import com.financecoach.dto.request.AddToCartRequest;
import com.financecoach.dto.response.CartItemResponse;
import com.financecoach.dto.response.SmartCartResponse;

import java.util.List;

public interface CartService {

    /** Sepete ürün ekle (aynı ürün varsa miktarı artır) */
    CartItemResponse addToCart(AddToCartRequest request);

    /** Kullanıcının aktif sepetini getir */
    List<CartItemResponse> getCartItems();

    /** Akıllı sepet — market bazlı dağıtım + tasarruf hesabı */
    SmartCartResponse getSmartCart();

    /** Sepetten ürün sil */
    void removeFromCart(Long itemId);

    /** Ürün miktarını güncelle */
    CartItemResponse updateQuantity(Long itemId, int quantity);

    /** Sepeti temizle */
    void clearCart();

    /** Alışverişi tamamla (sepeti arşivle) */
    void completeCart();

    /** Sepetteki ürün sayısı */
    long getCartCount();
}