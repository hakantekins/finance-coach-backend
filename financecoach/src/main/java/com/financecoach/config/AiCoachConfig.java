package com.financecoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * AI Coach modülü için RestClient konfigürasyonu.
 * <p>
 * Spring Boot 3.2'de tanıtılan RestClient, WebClient'ın
 * blocking alternatifi olarak önerilir. RestTemplate'in modern halefidir.
 * <p>
 * Bean olarak tanımlanması:
 * - Her servis çağrısında yeniden oluşturulmasını engeller
 * - Base URL ve Authorization header bir kez set edilir
 * - Test sırasında mock bean ile kolayca değiştirilebilir
 */
@Configuration
public class AiCoachConfig {

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.api-key}")
    private String apiKey;

    /**
     * OpenAI API standartlarına uygun RestClient bean'i.
     * Authorization header her istekte otomatik eklenir.
     *
     * @return Yapılandırılmış RestClient instance'ı
     */
    @Bean
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}