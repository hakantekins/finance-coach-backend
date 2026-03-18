package com.financecoach.service;

import com.financecoach.dto.response.AdminFeedbackItemResponse;
import com.financecoach.dto.request.FeedbackRequest;

import java.util.List;

public interface FeedbackService {

    void submitFeedback(FeedbackRequest request);

    List<AdminFeedbackItemResponse> getAllFeedbacks();
}

