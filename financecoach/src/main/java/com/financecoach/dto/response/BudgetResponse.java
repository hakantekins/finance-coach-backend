package com.financecoach.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private Long id;
    private String category;
    private BigDecimal monthlyLimit;

    /** Bu ay bu kategoride harcanan tutar */
    private BigDecimal currentSpent;

    /** Kalan bütçe (limit - harcanan) */
    private BigDecimal remaining;

    /** Kullanım yüzdesi (0-100+) — 100 üstü = aşım */
    private Double usagePercent;

    /** Bütçe aşıldı mı? */
    private boolean overBudget;

    /** Uyarı seviyesi: SAFE / WARNING / DANGER / OVER */
    private String status;

    private OffsetDateTime createdAt;
}