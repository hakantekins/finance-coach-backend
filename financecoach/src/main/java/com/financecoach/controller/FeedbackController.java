package com.financecoach.controller;

import com.financecoach.dto.request.FeedbackRequest;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Auth gereklidir.
     * POST /api/v1/feedback
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request
    ) {
        log.debug("POST /v1/feedback: mesaj uzunluğu={}",
                request.getMessage() == null ? 0 : request.getMessage().length());

        feedbackService.submitFeedback(request);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Geri bildiriminiz alındı. Teşekkürler!")
        );
    }
}

