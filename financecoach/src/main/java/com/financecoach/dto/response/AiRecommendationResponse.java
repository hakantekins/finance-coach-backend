package com.financecoach.dto.response;

import com.financecoach.model.entity.AiRecommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * AI tavsiye endpoint'lerinden istemciye dönen yanıt DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationResponse {

    private Long id;
    private Long userId; // UUID yerine Long olarak güncelledik
    private AiRecommendation.RecommendationType type;

    /** Kullanıcıya gösterilecek tavsiye metni */
    private String message;

    /** Hangi kategorinin analiz edildiği (null olabilir) */
    private String analyzedCategory;

    private OffsetDateTime createdAt;
}