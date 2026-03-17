package com.financecoach.service.impl;

import com.financecoach.dto.request.UpcomingPaymentRequest;
import com.financecoach.dto.response.UpcomingPaymentResponse;
import com.financecoach.exception.ResourceNotFoundException;
import com.financecoach.model.entity.UpcomingPayment;
import com.financecoach.model.entity.User;
import com.financecoach.repository.UpcomingPaymentRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.UpcomingPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UpcomingPaymentServiceImpl extends BaseAuthService implements UpcomingPaymentService {

    private final UpcomingPaymentRepository paymentRepository;

    @Override
    @Transactional
    public UpcomingPaymentResponse createPayment(UpcomingPaymentRequest request) {
        User currentUser = getAuthenticatedUser();

        UpcomingPayment payment = UpcomingPayment.builder()
                .userId(currentUser.getId())
                .title(request.getTitle())
                .category(request.getCategory())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .creditLimit(request.getCreditLimit())
                .description(request.getDescription())
                .isRecurring(request.isRecurring())
                .build();

        UpcomingPayment saved = paymentRepository.save(payment);
        log.debug("Ödeme oluşturuldu: id={}, title={}", saved.getId(), saved.getTitle());
        return mapToResponse(saved);
    }

    @Override
    public List<UpcomingPaymentResponse> getUserPayments() {
        User currentUser = getAuthenticatedUser();
        return paymentRepository
                .findByUserIdOrderByDueDateAsc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UpcomingPaymentResponse> getPendingPayments() {
        User currentUser = getAuthenticatedUser();
        return paymentRepository
                .findByUserIdAndIsPaidFalseOrderByDueDateAsc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UpcomingPaymentResponse> getUrgentPayments() {
        User currentUser = getAuthenticatedUser();
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        return paymentRepository
                .findUpcomingByDateRange(currentUser.getId(), today, threeDaysLater)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UpcomingPaymentResponse markAsPaid(Long id) {
        User currentUser = getAuthenticatedUser();

        UpcomingPayment payment = paymentRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ödeme bulunamadı: id=" + id));

        payment.setPaid(true);

        // Tekrarlayan ödeme ise bir sonraki ay için yeni kayıt oluştur
        if (payment.isRecurring()) {
            UpcomingPayment nextPayment = UpcomingPayment.builder()
                    .userId(currentUser.getId())
                    .title(payment.getTitle())
                    .category(payment.getCategory())
                    .amount(payment.getAmount())
                    .dueDate(payment.getDueDate().plusMonths(1))
                    .creditLimit(payment.getCreditLimit())
                    .description(payment.getDescription())
                    .isRecurring(true)
                    .build();
            paymentRepository.save(nextPayment);
            log.debug("Tekrarlayan ödeme için sonraki ay kaydı oluşturuldu: {}",
                    nextPayment.getDueDate());
        }

        return mapToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        User currentUser = getAuthenticatedUser();
        paymentRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ödeme bulunamadı: id=" + id));
        paymentRepository.deleteById(id);
    }

    // ─── Private ────────────────────────────────────────────────────────────


    private UpcomingPaymentResponse mapToResponse(UpcomingPayment p) {
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, p.getDueDate());
        boolean isOverdue = daysUntilDue < 0 && !p.isPaid();
        boolean isUrgent  = daysUntilDue >= 0 && daysUntilDue <= 3 && !p.isPaid();

        return UpcomingPaymentResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .category(p.getCategory())
                .amount(p.getAmount())
                .dueDate(p.getDueDate())
                .isPaid(p.isPaid())
                .creditLimit(p.getCreditLimit())
                .description(p.getDescription())
                .isRecurring(p.isRecurring())
                .daysUntilDue(daysUntilDue)
                .isOverdue(isOverdue)
                .isUrgent(isUrgent)
                .createdAt(p.getCreatedAt())
                .build();
    }
}