package com.financecoach.service.impl;

import com.financecoach.dto.request.CreateTransactionRequest;
import com.financecoach.dto.response.BalanceResponse;
import com.financecoach.dto.response.TransactionResponse;
import com.financecoach.exception.ResourceNotFoundException;
import com.financecoach.model.entity.Transaction;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.TransactionType;
import com.financecoach.repository.TransactionRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionServiceImpl extends BaseAuthService implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        User currentUser = getAuthenticatedUser();

        Transaction transaction = Transaction.builder()
                .user(currentUser)
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(
                        request.getTransactionDate() != null
                                ? request.getTransactionDate()
                                : LocalDate.now()
                )
                .isFixed(request.isFixed())
                .isRecurring(request.isRecurring())
                .recurringDay(request.getRecurringDay())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.debug("İşlem oluşturuldu: id={}, tip={}, sabit={}",
                saved.getId(), saved.getType(), saved.isFixed());
        return mapToResponse(saved);
    }

    @Override
    public List<TransactionResponse> getUserTransactions() {
        User currentUser = getAuthenticatedUser();
        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Sayfalama destekli işlem listesi.
     * Controller'dan Pageable ile çağrılır: ?page=0&size=20&sort=transactionDate,desc
     */
    @Override
    public Page<TransactionResponse> getUserTransactions(Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(currentUser.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    public BalanceResponse getUserBalance() {
        User currentUser = getAuthenticatedUser();

        BigDecimal totalIncome = transactionRepository
                .findTotalByUserIdAndType(currentUser.getId(), TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository
                .findTotalByUserIdAndType(currentUser.getId(), TransactionType.EXPENSE);

        totalIncome  = totalIncome  != null ? totalIncome  : BigDecimal.ZERO;
        totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;

        return BalanceResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .build();
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        User currentUser = getAuthenticatedUser();
        transactionRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "İşlem bulunamadı veya bu işlem size ait değil: id=" + id));
        transactionRepository.deleteById(id);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .userId(transaction.getUser().getId())
                .isFixed(transaction.isFixed())
                .isRecurring(transaction.isRecurring())
                .recurringDay(transaction.getRecurringDay())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}