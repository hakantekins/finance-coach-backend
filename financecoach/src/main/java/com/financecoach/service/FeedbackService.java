package com.financecoach.service;

import com.financecoach.dto.request.FeedbackRequest;

public interface FeedbackService {

    void submitFeedback(FeedbackRequest request);
}

