package com.financecoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequest {

    @NotBlank(message = "Geri bildirim mesajı boş olamaz")
    @Size(max = 2000, message = "Mesaj en fazla 2000 karakter olabilir")
    private String message;
}

