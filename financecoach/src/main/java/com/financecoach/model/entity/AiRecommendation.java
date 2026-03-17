package com.financecoach.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


/**
 * Yapay zeka tarafından üretilen finansal tavsiyeleri saklayan entity.
 * <p>
 * Tasarım kararları:
 * - id: Long (MarketPrice ile tutarlı; bu modül de bağımsız referans veri)
 * - userId: UUID (User entity ile FK ilişkisi yok; sadece sahiplik referansı.
 *   User silinse bile tavsiye geçmişi korunur — audit amaçlı.)
 * - message: TEXT (tavsiye metni değişken uzunlukta olabilir)
 * - type: TAVSİYENİN TÜRÜNÜ belirtir (GENERAL / MARKET_COMPARISON / CATEGORY_ALERT)
 */
@Entity
@Table(
        name = "ai_recommendations",
        indexes = {
                // Kullanıcının tavsiye geçmişini tarihe göre çekmek için
                @Index(name = "idx_ai_rec_user_id",      columnList = "user_id"),
                @Index(name = "idx_ai_rec_user_created",  columnList = "user_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tavsiyenin sahibi kullanıcının UUID'si.
     * Kasıtlı olarak @ManyToOne/FK yerine plain UUID tutulur:
     * - User silinse tavsiye geçmişi kaybolmaz (soft audit trail)
     * - JOIN maliyeti olmadan hızlı filtreleme yapılır
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Tavsiye türü — PromptGenerator hangi akışı kullandığını işaretler.
     * GENERAL           : Genel gelir/gider analizi
     * MARKET_COMPARISON : Market fiyat karşılaştırması içeren tavsiye
     * CATEGORY_ALERT    : Belirli bir kategoride aşırı harcama uyarısı
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RecommendationType type = RecommendationType.GENERAL;

    /** LLM veya simülasyon tarafından üretilen tavsiye metni */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Tavsiye üretilirken kullanılan prompt.
     * Debug, audit ve prompt iyileştirme için saklanır.
     * Prod'da gizlenebilir (DTO'ya dahil edilmez).
     */
    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    /** Tavsiyenin hangi kategori analiz edilerek üretildiği */
    @Column(name = "analyzed_category", length = 100)
    private String analyzedCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ─── İç Enum ─────────────────────────────────────────────────────────────

    public enum RecommendationType {
        GENERAL,
        MARKET_COMPARISON,
        CATEGORY_ALERT
    }
}