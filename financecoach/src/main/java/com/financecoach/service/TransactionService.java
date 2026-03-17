package com.financecoach.service;

import com.financecoach.dto.request.CreateTransactionRequest;
import com.financecoach.dto.response.BalanceResponse;
import com.financecoach.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

    /**
     * Transaction iş mantığı için servis arayüzü.
     * Kullanıcı bilgisi SecurityContextHolder'dan alındığı için
     * metodlara ayrıca User parametresi geçilmez.
     */
    public interface TransactionService {
        Page<TransactionResponse> getUserTransactions(Pageable pageable);
        /**
         * Yeni işlem (gelir veya gider) ekler.
         * Kullanıcı kimliği SecurityContext'ten alınır.
         *
         * @param request Validasyonlu işlem isteği
         * @return Oluşturulan işlemin DTO'su
         */
        TransactionResponse createTransaction(CreateTransactionRequest request);

        /**
         * Oturum açmış kullanıcının tüm işlemlerini listeler.
         *
         * @return İşlem listesi (tarihe göre azalan)
         */
        List<TransactionResponse> getUserTransactions();

        /**
         * Kullanıcının anlık bakiyesini hesaplar.
         * Bakiye = Toplam Gelir - Toplam Gider
         *
         * @return totalIncome, totalExpense, balance içeren DTO
         */
        BalanceResponse getUserBalance();
        // Sadece bu satırı eklemen yeterli:
        void deleteTransaction(Long id);
    }

