package com.financecoach.service;

import com.financecoach.dto.response.DashboardResponse;

/**
 * Dashboard özet verisi için servis arayüzü.
 * Kullanıcı kimliği SecurityContextHolder'dan alınır.
 */
public interface DashboardService {

    /**
     * Oturum açmış kullanıcının dashboard özetini döner.
     * Veri kaynağı: {@code v_user_dashboard_summary} PostgreSQL VIEW'ı.
     *
     * @return Toplam gelir/gider, net bakiye, bu ay harcaması,
     *         tasarruf hedefi ilerlemesi ve akıllı harcama skoru
     */
    DashboardResponse getDashboard();
}