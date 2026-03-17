package com.financecoach.service;

import com.financecoach.dto.request.LoginRequest;
import com.financecoach.dto.request.RegisterRequest;
import com.financecoach.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}