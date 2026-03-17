package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * GET /api/v1/analytics/savings endpoint'inden dönen aylık birikim DTO'su.
 *
 * Frontend Recharts LineChart ile doğrudan uyumlu format:
 * { month: "Oca", savings: 3250.00, income: 15000, expense: 11750 }
 *
 * savings   → o aya kadar birikmiş KÜMÜLATİF net bakiye
 * monthlyNet → sadece o aydaki net (gelir - gider)
 * income    → o aydaki toplam gelir
 * expense   → o aydaki toplam gider
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySavingsResponse {

    /** Recharts XAxis label: "Oca", "Şub", "Mar" ... "Ara" */
    private String month;

    /** Kümülatif birikim — grafiğin y ekseni bu değeri kullanır */
    private BigDecimal savings;

    /** Sadece o aydaki net (pozitif = kazandı, negatif = açık verdi) */
    private BigDecimal monthlyNet;

    /** O aydaki toplam INCOME */
    private BigDecimal income;

    /** O aydaki toplam EXPENSE */
    private BigDecimal expense;
}