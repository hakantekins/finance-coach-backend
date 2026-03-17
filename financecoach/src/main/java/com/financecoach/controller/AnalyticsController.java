package com.financecoach.controller;

import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.MonthlySavingsResponse;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.TransactionType;
import com.financecoach.repository.TransactionRepository;
import com.financecoach.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final TransactionRepository transactionRepository;
    private final AnalyticsService analyticsService; // YENİ: sadece savings icin eklendi

    // ─── Birikim grafigi (GERCEK VERI) ───────────────────────────────────────
    // DEGISIKLIK: mock data kaldirildi, AnalyticsService'ten gercek veri cekiliyor
    @GetMapping("/savings")
    public ResponseEntity<ApiResponse<List<MonthlySavingsResponse>>> getSavingsAnalytics() {
        log.debug("GET /v1/analytics/savings");
        List<MonthlySavingsResponse> data = analyticsService.getMonthlySavings();
        return ResponseEntity.ok(ApiResponse.success(data, "Grafik verileri yuklendi"));
    }

    // ─── Kategori bazli pasta grafigi ────────────────────────────────────────
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryBreakdown() {
        User user = getAuthenticatedUser();

        List<Object[]> results = transactionRepository
                .findCategoryTotalsByUserId(user.getId());

        BigDecimal total = results.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> data = results.stream()
                .map(r -> {
                    String category = r[0] != null ? r[0].toString() : "Diger";
                    BigDecimal amount = (BigDecimal) r[1];
                    double pct = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(total, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                            : 0.0;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name",    category);
                    map.put("value",   amount);
                    map.put("percent", Math.round(pct * 10.0) / 10.0);
                    return map;
                })
                .sorted((a, b) -> Double.compare(
                        (double) b.get("percent"),
                        (double) a.get("percent")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(data, "Kategori dagilimi hazirlandi"));
    }

    // ─── Aylik gelir vs gider (son 6 ay) ─────────────────────────────────────
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyComparison() {
        User user = getAuthenticatedUser();

        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate month     = now.minusMonths(i);
            LocalDate startDate = month.withDayOfMonth(1);
            LocalDate endDate   = month.withDayOfMonth(month.lengthOfMonth());

            BigDecimal income = transactionRepository
                    .findTotalByUserIdAndTypeAndDateRange(
                            user.getId(), TransactionType.INCOME, startDate, endDate);
            BigDecimal expense = transactionRepository
                    .findTotalByUserIdAndTypeAndDateRange(
                            user.getId(), TransactionType.EXPENSE, startDate, endDate);

            income  = income  != null ? income  : BigDecimal.ZERO;
            expense = expense != null ? expense : BigDecimal.ZERO;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("month",   month.getMonth()
                    .getDisplayName(TextStyle.SHORT, new Locale("tr")));
            map.put("income",  income);
            map.put("expense", expense);
            map.put("net",     income.subtract(expense));
            data.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Aylik karsilastirma hazirlandi"));
    }

    // ─── En yuksek 5 harcama ─────────────────────────────────────────────────
    @GetMapping("/top-expenses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopExpenses() {
        User user = getAuthenticatedUser();

        List<Object[]> results = transactionRepository
                .findTopExpensesByUserId(user.getId(), 5);

        List<Map<String, Object>> data = results.stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("category",        r[0] != null ? r[0].toString() : "Diger");
                    map.put("totalAmount",      r[1]);
                    map.put("transactionCount", r[2]);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(data, "En yuksek harcamalar listelendi"));
    }

    // ─── Haftalik istatistikler ───────────────────────────────────────────────
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyStats() {
        User user = getAuthenticatedUser();

        LocalDate today         = LocalDate.now();
        LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate thisWeekEnd   = today.with(DayOfWeek.SUNDAY);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd   = thisWeekStart.minusDays(1);

        BigDecimal thisWeekExpense = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(
                        user.getId(), TransactionType.EXPENSE, thisWeekStart, thisWeekEnd);
        BigDecimal thisWeekIncome = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(
                        user.getId(), TransactionType.INCOME, thisWeekStart, thisWeekEnd);
        BigDecimal lastWeekExpense = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(
                        user.getId(), TransactionType.EXPENSE, lastWeekStart, lastWeekEnd);
        BigDecimal lastWeekIncome = transactionRepository
                .findTotalByUserIdAndTypeAndDateRange(
                        user.getId(), TransactionType.INCOME, lastWeekStart, lastWeekEnd);

        thisWeekExpense = thisWeekExpense != null ? thisWeekExpense : BigDecimal.ZERO;
        thisWeekIncome  = thisWeekIncome  != null ? thisWeekIncome  : BigDecimal.ZERO;
        lastWeekExpense = lastWeekExpense != null ? lastWeekExpense : BigDecimal.ZERO;
        lastWeekIncome  = lastWeekIncome  != null ? lastWeekIncome  : BigDecimal.ZERO;

        double expenseChangePct = lastWeekExpense.compareTo(BigDecimal.ZERO) > 0
                ? thisWeekExpense.subtract(lastWeekExpense)
                .divide(lastWeekExpense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        double incomeChangePct = lastWeekIncome.compareTo(BigDecimal.ZERO) > 0
                ? thisWeekIncome.subtract(lastWeekIncome)
                .divide(lastWeekIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        long transactionCount = transactionRepository
                .countByUserIdAndDateRange(user.getId(), thisWeekStart, thisWeekEnd);

        long daysPassed = ChronoUnit.DAYS.between(thisWeekStart, today) + 1;
        BigDecimal dailyAvg = daysPassed > 0
                ? thisWeekExpense.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thisWeekExpense",  thisWeekExpense);
        data.put("thisWeekIncome",   thisWeekIncome);
        data.put("lastWeekExpense",  lastWeekExpense);
        data.put("lastWeekIncome",   lastWeekIncome);
        data.put("expenseChangePct", Math.round(expenseChangePct * 10.0) / 10.0);
        data.put("incomeChangePct",  Math.round(incomeChangePct  * 10.0) / 10.0);
        data.put("transactionCount", transactionCount);
        data.put("dailyAvgExpense",  dailyAvg);
        data.put("weekStartDate",    thisWeekStart.toString());
        data.put("weekEndDate",      thisWeekEnd.toString());

        return ResponseEntity.ok(ApiResponse.success(data, "Haftalik istatistikler hazirlandi"));
    }

    // ─── Private ─────────────────────────────────────────────────────────────
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
    // ─── Haftalık günlük harcama (Pzt-Paz bar chart) ─────────────────────────
    @GetMapping("/weekly-daily")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWeeklyDailyExpenses() {
        User user = getAuthenticatedUser();

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        String[] gunler = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);

            BigDecimal expense = transactionRepository
                    .findTotalByUserIdAndTypeAndDateRange(
                            user.getId(), TransactionType.EXPENSE, day, day);

            expense = expense != null ? expense : BigDecimal.ZERO;

            boolean isToday = day.equals(today);
            boolean isFuture = day.isAfter(today);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("day", gunler[i]);
            map.put("date", day.toString());
            map.put("amount", expense);
            map.put("isToday", isToday);
            map.put("isFuture", isFuture);
            data.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Haftalık günlük harcamalar"));
    }
}