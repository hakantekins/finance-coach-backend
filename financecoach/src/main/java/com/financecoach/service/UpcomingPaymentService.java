package com.financecoach.service;

import com.financecoach.dto.request.UpcomingPaymentRequest;
import com.financecoach.dto.response.UpcomingPaymentResponse;

import java.util.List;

public interface UpcomingPaymentService {

    /** Yeni ödeme ekle */
    UpcomingPaymentResponse createPayment(UpcomingPaymentRequest request);

    /** Kullanıcının tüm ödemeleri */
    List<UpcomingPaymentResponse> getUserPayments();

    /** Sadece ödenmemiş olanlar */
    List<UpcomingPaymentResponse> getPendingPayments();

    /** 3 gün içinde vadesi dolacaklar — uyarı için */
    List<UpcomingPaymentResponse> getUrgentPayments();

    /** Ödendi olarak işaretle */
    UpcomingPaymentResponse markAsPaid(Long id);

    /** Ödemeyi sil */
    void deletePayment(Long id);
}