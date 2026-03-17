package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kullanıcının bakiye özetini dönen veri transfer objesi (DTO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private BigDecimal totalIncome;  // Toplam Gelir
    private BigDecimal totalExpense; // Toplam Gider
    private BigDecimal balance;      // Net Bakiye (Gelir - Gider)
}