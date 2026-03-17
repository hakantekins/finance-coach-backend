package com.financecoach.service.impl;

import com.financecoach.dto.response.MonthlySavingsResponse;
import com.financecoach.model.entity.Transaction;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.TransactionType;
import com.financecoach.repository.AnalyticsRepository;
import com.financecoach.service.AnalyticsService;
import com.financecoach.service.BaseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl extends BaseAuthService implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    private static final int MONTHS_TO_SHOW = 12;

    private static final String[] TR_MONTHS = {
            "Oca", "Sub", "Mar", "Nis", "May", "Haz",
            "Tem", "Agu", "Eyl", "Eki", "Kas", "Ara"
    };

    @Override
    public List<MonthlySavingsResponse> getMonthlySavings() {
        User currentUser = getAuthenticatedUser();

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth   = currentMonth.minusMonths(MONTHS_TO_SHOW - 1);

        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate   = currentMonth.atEndOfMonth();

        log.debug("Analytics: userId={}, aralik={}/{}",
                currentUser.getId(), startDate, endDate);

        // User.id Long tipinde — Long olarak geçiyoruz
        List<Transaction> transactions = analyticsRepository
                .findByUserIdAndDateRange(currentUser.getId(), startDate, endDate);

        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t ->
                        YearMonth.from(t.getTransactionDate())));

        List<MonthlySavingsResponse> result = new ArrayList<>();
        BigDecimal cumulativeSavings = BigDecimal.ZERO;

        for (int i = 0; i < MONTHS_TO_SHOW; i++) {
            YearMonth month   = startMonth.plusMonths(i);
            List<Transaction> monthTxs = byMonth.getOrDefault(month, List.of());

            BigDecimal monthIncome = monthTxs.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthExpense = monthTxs.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthlyNet  = monthIncome.subtract(monthExpense);
            cumulativeSavings      = cumulativeSavings.add(monthlyNet);

            result.add(MonthlySavingsResponse.builder()
                    .month(TR_MONTHS[month.getMonthValue() - 1])
                    .savings(cumulativeSavings)
                    .monthlyNet(monthlyNet)
                    .income(monthIncome)
                    .expense(monthExpense)
                    .build());
        }

        log.debug("Analytics tamamlandi: {} ay, final={}", result.size(), cumulativeSavings);
        return result;
    }

}