package com.financecoach.repository;

import com.financecoach.model.entity.Transaction;
import com.financecoach.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    /** Pageable versiyon — sayfalama destekli */
    Page<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.email = :email
        ORDER BY t.transactionDate DESC
    """)
    List<Transaction> findAllByUserEmail(@Param("email") String email);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
    """)
    BigDecimal findTotalByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") TransactionType type
    );

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id   = :userId
          AND t.type      = :type
          AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal findTotalByUserIdAndTypeAndDateRange(
            @Param("userId")    Long userId,
            @Param("type")      TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    @Query("""
        SELECT t.category, SUM(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type    = 'EXPENSE'
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
    """)
    List<Object[]> findCategoryTotalsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT t.category, SUM(t.amount), COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type    = 'EXPENSE'
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        LIMIT :limit
    """)
    List<Object[]> findTopExpensesByUserId(
            @Param("userId") Long userId,
            @Param("limit")  int limit
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    long countByUserIdAndDateRange(
            @Param("userId")    Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    List<Transaction> findByUserIdAndIsRecurringTrueOrderByRecurringDayAsc(Long userId);
}