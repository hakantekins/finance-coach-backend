package com.financecoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth endpoint'lerinden istemciye dönen JWT token yanıtı.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** JWT access token */
    private String token;

    /** Her zaman "Bearer" - istemci Authorization header'ını buna göre oluşturur */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Frontend'in kullanıcı bilgilerini ayrıca fetch etmemesi için eklendi */
    private String email;
    private String fullName;
}