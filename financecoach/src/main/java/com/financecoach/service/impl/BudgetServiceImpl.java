package com.financecoach.service.impl;

import com.financecoach.dto.request.BudgetRequest;
import com.financecoach.dto.response.BudgetResponse;
import com.financecoach.model.entity.Budget;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.TransactionType;
import com.financecoach.repository.BudgetRepository;
import com.financecoach.repository.TransactionRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BudgetServiceImpl extends BaseAuthService implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public BudgetResponse createOrUpdateBudget(BudgetRequest request) {
        User user = getAuthenticatedUser();

        // Aynı kategori varsa güncelle, yoksa oluştur
        Optional<Budget> existing = budgetRepository
                .findByUserIdAndCategoryIgnoreCase(user.getId(), request.getCategory().trim());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setMonthlyLimit(request.getMonthlyLimit());
            log.debug("Bütçe güncellendi: kategori='{}', limit={}", budget.getCategory(), budget.getMonthlyLimit());
        } else {
            budget = Budget.builder()
                    .userId(user.getId())
                    .category(request.getCategory().trim())
                    .monthlyLimit(request.getMonthlyLimit())
                    .build();
            log.debug("Yeni bütçe oluşturuldu: kategori='{}', limit={}", budget.getCategory(), budget.getMonthlyLimit());
        }

        Budget saved = budgetRepository.save(budget);
        return buildResponse(saved, user.getId());
    }

    @Override
    public List<BudgetResponse> getUserBudgets() {
        User user = getAuthenticatedUser();
        return budgetRepository.findByUserIdOrderByCategoryAsc(user.getId())
                .stream()
                .map(b -> buildResponse(b, user.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetResponse> getOverBudgetAlerts() {
        User user = getAuthenticatedUser();
        return budgetRepository.findByUserIdOrderByCategoryAsc(user.getId())
                .stream()
                .map(b -> buildResponse(b, user.getId()))
                .filter(b -> b.getUsagePercent() >= 80.0) // %80 ve üstü uyarı
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        User user = getAuthenticatedUser();
        budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı: id=" + id));
        budgetRepository.deleteById(id);
        log.debug("Bütçe silindi: id={}", id);
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private BudgetResponse buildResponse(Budget budget, Long userId) {
        // Bu ay bu kategorideki toplam harcamayı hesapla
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        BigDecimal spent = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, monthStart, monthEnd);

        // Kategori bazlı filtreleme — transaction'larda category ile eşleştir
        // Not: findTotalByUserIdAndTypeAndDateRange tüm EXPENSE'leri toplar
        // Kategori bazlı toplam için özel sorgu lazım, şimdilik bu kategorinin
        // transaction'larını filtreleyelim
        BigDecimal categorySpent = calculateCategorySpent(userId, budget.getCategory(), monthStart, monthEnd);

        BigDecimal remaining = budget.getMonthlyLimit().subtract(categorySpent);

        double usagePercent = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                ? categorySpent.divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        boolean overBudget = usagePercent > 100.0;

        String status;
        if (usagePercent > 100) status = "OVER";
        else if (usagePercent >= 80) status = "DANGER";
        else if (usagePercent >= 60) status = "WARNING";
        else status = "SAFE";

        return BudgetResponse.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .monthlyLimit(budget.getMonthlyLimit())
                .currentSpent(categorySpent)
                .remaining(remaining)
                .usagePercent(Math.round(usagePercent * 10.0) / 10.0)
                .overBudget(overBudget)
                .status(status)
                .createdAt(budget.getCreatedAt())
                .build();
    }

    private BigDecimal calculateCategorySpent(Long userId, String category, LocalDate start, LocalDate end) {
        // TransactionRepository'deki mevcut sorguları kullan
        // Kategori bazlı toplam: findCategoryTotalsByUserId zaten var ama tarih filtresi yok
        // Şimdilik tüm EXPENSE transaction'ları çekip Java'da filtrele
        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(userId)
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getCategory() != null && t.getCategory().equalsIgnoreCase(category))
                .filter(t -> !t.getTransactionDate().isBefore(start) && !t.getTransactionDate().isAfter(end))
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}