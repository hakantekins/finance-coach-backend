package com.financecoach.controller;

import com.financecoach.dto.response.AdminFeedbackItemResponse;
import com.financecoach.dto.response.ApiResponse;
import com.financecoach.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/feedbacks")
@RequiredArgsConstructor
@Slf4j
public class FeedbackAdminController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminFeedbackItemResponse>>> getAllFeedbacks() {
        log.debug("GET /v1/feedbacks");
        List<AdminFeedbackItemResponse> items = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(
                ApiResponse.success(items, "Geri bildirimler listelendi")
        );
    }
}

