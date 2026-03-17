package com.financecoach.repository;

import com.financecoach.model.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByCategoryAsc(Long userId);

    Optional<Budget> findByUserIdAndCategoryIgnoreCase(Long userId, String category);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}