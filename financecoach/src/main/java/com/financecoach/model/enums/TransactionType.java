package com.financecoach.model.enums;

/**
 * İşlem tipi.
 * INCOME → Gelir (maaş, ek gelir vb.)
 * EXPENSE → Gider (harcama)
 *
 * Bakiye hesabı: SUM(INCOME) - SUM(EXPENSE)
 */
public enum TransactionType {
    INCOME,
    EXPENSE
}