package com.financecoach.repository;

import com.financecoach.model.entity.UpcomingPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UpcomingPaymentRepository extends JpaRepository<UpcomingPayment, Long> {

    /** Kullanıcının tüm ödemelerini tarihe göre sıralar */
    List<UpcomingPayment> findByUserIdOrderByDueDateAsc(Long userId);

    /** Sadece ödenmemiş olanlar */
    List<UpcomingPayment> findByUserIdAndIsPaidFalseOrderByDueDateAsc(Long userId);

    /** Belirli tarih aralığındaki ödemeler — uyarı sistemi için */
    @Query("""
        SELECT p FROM UpcomingPayment p
        WHERE p.userId = :userId
          AND p.isPaid = false
          AND p.dueDate BETWEEN :start AND :end
        ORDER BY p.dueDate ASC
    """)
    List<UpcomingPayment> findUpcomingByDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /** Güvenlik: Bu ödeme bu kullanıcıya mı ait? */
    Optional<UpcomingPayment> findByIdAndUserId(Long id, Long userId);
}