package com.financecoach.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "cart_items",
        indexes = {
                @Index(name = "idx_cart_user_id", columnList = "user_id"),
                @Index(name = "idx_cart_completed", columnList = "user_id, is_completed")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sepet sahibi kullanıcı */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Kullanıcının eklediği ürün adı.
     * MarketPrice.productName ile eşleştirilir.
     * Örnek: "Tam Yağlı Süt", "Beyaz Peynir"
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /**
     * Miktar — kaç adet/birim alınacak.
     * Default: 1
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Alışveriş tamamlandı mı?
     * true = kullanıcı "Alışverişi Tamamla" butonuna bastı
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean completed = false;

    /**
     * Tamamlanma tarihi — alışveriş geçmişi için
     */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}