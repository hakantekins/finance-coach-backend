package com.financecoach.service.impl;

import com.financecoach.dto.request.LoginRequest;
import com.financecoach.dto.request.RegisterRequest;
import com.financecoach.dto.response.AuthResponse;
import com.financecoach.model.entity.User;
import com.financecoach.model.enums.UserRole;
import com.financecoach.repository.UserRepository;
import com.financecoach.security.JwtService; // Aradaki .jwt kısmını sildik
import com.financecoach.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * AuthService implementasyonu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. E-posta kontrolü
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu e-posta adresi zaten kayıtlı: " + request.getEmail());
        }

        // 2. User entity oluşturma
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .monthlyIncome(request.getMonthlyIncome() != null ? request.getMonthlyIncome() : BigDecimal.ZERO)
                .monthlySavingsGoal(request.getMonthlySavingsGoal() != null ? request.getMonthlySavingsGoal() : BigDecimal.ZERO)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Yeni kullanıcı kaydedildi: email={}", savedUser.getEmail());

        // 3. Token üretimi
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Kimlik doğrulama
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Kullanıcıyı çekme
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + request.getEmail()));

        log.info("Kullanıcı giriş yaptı: email={}", user.getEmail());

        // 3. Token üretimi
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}