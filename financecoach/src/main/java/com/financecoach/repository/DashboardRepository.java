package com.financecoach.repository;

import com.financecoach.model.entity.DashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DashboardSummary VIEW entity'si için read-only repository.
 * ID tipini projenin geneline uygun olarak Long yaptık.
 */
@Repository
public interface DashboardRepository extends JpaRepository<DashboardSummary, Long> {

    /**
     * Kullanıcı ID'sine göre dashboard özetini döner.
     */
    Optional<DashboardSummary> findByUserId(Long userId);
}