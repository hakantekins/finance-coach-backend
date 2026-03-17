package com.financecoach.config;

import com.financecoach.model.entity.Transaction;
import com.financecoach.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Tekrarlayan işlemleri otomatik oluşturan scheduler.
 * Her gün 00:05'te çalışır.
 * Bugünün günü ile recurringDay eşleşen işlemler için
 * bu ayda henüz oluşturulmamışsa yeni kayıt ekler.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final TransactionRepository transactionRepository;

    @Scheduled(cron = "0 5 0 * * *") // Her gün 00:05
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        int todayDay = today.getDayOfMonth();

        log.info("Tekrarlayan işlemler kontrol ediliyor: gün={}", todayDay);

        // Tüm tekrarlayan işlemleri bul
        List<Transaction> allRecurring = transactionRepository.findAll().stream()
                .filter(t -> t.isRecurring() && t.getRecurringDay() != null)
                .filter(t -> t.getRecurringDay() == todayDay)
                .toList();

        int created = 0;

        for (Transaction template : allRecurring) {
            // Bu ay bu işlem zaten oluşturulmuş mu?
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            boolean alreadyExists = transactionRepository
                    .findByUserIdOrderByTransactionDateDesc(template.getUser().getId())
                    .stream()
                    .anyMatch(t -> !t.isRecurring()
                            && t.getCategory() != null
                            && t.getCategory().equals(template.getCategory())
                            && t.getAmount().compareTo(template.getAmount()) == 0
                            && t.getType() == template.getType()
                            && !t.getTransactionDate().isBefore(monthStart)
                            && !t.getTransactionDate().isAfter(monthEnd));

            if (alreadyExists) {
                continue;
            }

            // Yeni işlem oluştur (template'den kopyala)
            Transaction newTransaction = Transaction.builder()
                    .user(template.getUser())
                    .amount(template.getAmount())
                    .type(template.getType())
                    .category(template.getCategory())
                    .description("🔄 Otomatik: " + (template.getDescription() != null ? template.getDescription() : template.getCategory()))
                    .transactionDate(today)
                    .isFixed(template.isFixed())
                    .isRecurring(false) // Kopyası tekrarlayan değil, tek seferlik
                    .build();

            transactionRepository.save(newTransaction);
            created++;
            log.debug("Tekrarlayan işlem oluşturuldu: userId={}, kategori={}, tutar={}",
                    template.getUser().getId(), template.getCategory(), template.getAmount());
        }

        log.info("Tekrarlayan işlemler tamamlandı: {} yeni işlem oluşturuldu", created);
    }
}