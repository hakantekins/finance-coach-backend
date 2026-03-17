package com.financecoach.service;

import com.financecoach.dto.request.BudgetRequest;
import com.financecoach.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse createOrUpdateBudget(BudgetRequest request);

    List<BudgetResponse> getUserBudgets();

    /** Aşılan bütçeleri döner (Dashboard uyarısı için) */
    List<BudgetResponse> getOverBudgetAlerts();

    void deleteBudget(Long id);
}