package com.financecoach.controller;

import com.financecoach.dto.request.CreateTransactionRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.dto.response.BalanceResponse;
import com.financecoach.dto.response.TransactionResponse;
import com.financecoach.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        log.debug("POST /v1/transactions: tip={}, tutar={}", request.getType(), request.getAmount());
        TransactionResponse created = transactionService.createTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "İşlem başarıyla eklendi"));
    }

    /**
     * GET /v1/transactions
     * Sayfalama destekli — opsiyonel parametreler:
     *   ?page=0&size=20  → sayfalı sonuç (Page objesi ile totalPages, totalElements vb.)
     *   parametresiz      → eski davranış, tüm listeyi döner (geriye uyumlu)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUserTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        // Eğer page parametresi verilmişse → sayfalı sonuç dön
        if (page != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
            Page<TransactionResponse> result = transactionService.getUserTransactions(pageable);

            Map<String, Object> paged = Map.of(
                    "content", result.getContent(),
                    "page", result.getNumber(),
                    "size", result.getSize(),
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages(),
                    "hasNext", result.hasNext(),
                    "hasPrevious", result.hasPrevious()
            );

            return ResponseEntity.ok(ApiResponse.success(paged,
                    result.getTotalElements() + " işlemden " + result.getContent().size() + " tanesi listelendi"));
        }

        // Parametresiz → eski davranış (tüm liste)
        List<TransactionResponse> transactions = transactionService.getUserTransactions();
        return ResponseEntity.ok(
                ApiResponse.success(transactions, transactions.size() + " işlem listelendi"));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getUserBalance() {
        BalanceResponse balance = transactionService.getUserBalance();
        return ResponseEntity.ok(ApiResponse.success(balance, "Bakiye hesaplandı"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(null, "İşlem başarıyla silindi"));
    }
}