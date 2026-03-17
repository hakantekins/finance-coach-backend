package com.financecoach.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "market_prices",
        indexes = {
                @Index(name = "idx_mp_product_name", columnList = "product_name"),
                @Index(name = "idx_mp_market_name", columnList = "market_name"),
                @Index(name = "idx_mp_price_date", columnList = "price_date DESC"),
                @Index(name = "idx_mp_external_id", columnList = "external_id"),
                @Index(name = "idx_mp_category", columnList = "category")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ürün adı: "Tam Yağlı Süt", "Ekmek 500g", "Domates" */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** Market zinciri adı: "BİM", "ŞOK", "A101", "Migros" */
    @Column(name = "market_name", nullable = false, length = 100)
    private String marketName;

    /** Ürün fiyatı. Her zaman pozitif. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Fiyatın geçerli olduğu tarih */
    @Column(name = "price_date", nullable = false)
    @Builder.Default
    private LocalDate priceDate = LocalDate.now();

    /** Birim bilgisi: "1L", "1kg", "500g", "adet" */
    @Column(length = 50)
    private String unit;

    // ─── YENİ: Scraper İçin Eklenen Alanlar ─────────────────────────────────

    /**
     * Ürünün market sitesindeki benzersiz ID'si.
     * Duplikasyon kontrolü için kullanılır.
     * Örnek: "A101-26057089", "SOK-8539", "BIM-12345"
     */
    @Column(name = "external_id", length = 100)
    private String externalId;

    /**
     * Bir önceki fiyat — fiyat değişim takibi için.
     * Scraper güncelleme yaparken mevcut fiyatı buraya taşır.
     */
    @Column(name = "previous_price", precision = 10, scale = 2)
    private BigDecimal previousPrice;

    /**
     * Ürün detay sayfası URL'i.
     * Kullanıcı tıklayınca market sitesine yönlendirilir.
     */
    @Column(name = "product_url", length = 500)
    private String productUrl;

    /**
     * Ürün görseli URL'i (CDN).
     * Frontend'de ürün kartında gösterilir.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Ürün kategorisi: "Temel Gıda", "Süt Ürünleri", "Temizlik" vb.
     * Filtreleme ve gruplama için kullanılır.
     */
    @Column(length = 100)
    private String category;

    /**
     * Ürün hâlâ satışta mı?
     * Scraper ürünü bulamazsa false yapar, listeden çıkar.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Son scrape edilme zamanı.
     * Ne zaman güncellendiğini takip etmek için.
     */
    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;

    // ─── Mevcut Alanlar ─────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}