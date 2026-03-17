package com.financecoach.repository;

import com.financecoach.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Analytics modülüne özel transaction repository.
 * User.id  → Long (IDENTITY)
 * Transaction.id → Long (IDENTITY)
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND t.transactionDate >= :startDate
          AND t.transactionDate <= :endDate
        ORDER BY t.transactionDate ASC
    """)
    List<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}