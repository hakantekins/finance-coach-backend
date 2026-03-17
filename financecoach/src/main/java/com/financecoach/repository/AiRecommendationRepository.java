package com.financecoach.repository;

import com.financecoach.model.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI tavsiye geçmişi için repository.
 * ID tipi Long — AiRecommendation entity'siyle tutarlı.
 */
@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    /**
     * Kullanıcının tüm tavsiye geçmişini tarihe göre azalan sırada döner.
     * userId tipi Long olarak güncellendi.
     */
    List<AiRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Kullanıcının belirli türdeki tavsiyelerini listeler.
     */
    List<AiRecommendation> findByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId,
            AiRecommendation.RecommendationType type
    );

    /**
     * Kullanıcının toplam tavsiye sayısı.
     */
    long countByUserId(Long userId);
}