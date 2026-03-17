package com.financecoach.service;

import com.financecoach.model.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tüm authenticated service'lerin ortak base class'ı.
 * getAuthenticatedUser() tek yerde tanımlı, tüm service'ler miras alır.
 */
public abstract class BaseAuthService {

    protected User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Güvenlik bağlamında kullanıcı bulunamadı");
        }
        return (User) auth.getPrincipal();
    }
}