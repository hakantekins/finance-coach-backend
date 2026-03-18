package com.financecoach.service.impl;

import com.financecoach.dto.response.AdminFeedbackItemResponse;
import com.financecoach.dto.request.FeedbackRequest;
import com.financecoach.model.entity.Feedback;
import com.financecoach.model.entity.User;
import com.financecoach.repository.FeedbackRepository;
import com.financecoach.service.BaseAuthService;
import com.financecoach.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackServiceImpl extends BaseAuthService implements FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        User currentUser = getAuthenticatedUser();

        Feedback feedback = Feedback.builder()
                .userId(currentUser.getId())
                .message(request.getMessage().trim())
                .build();

        feedbackRepository.save(feedback);

        log.debug("Geri bildirim kaydedildi: userId={}, feedbackId={}",
                currentUser.getId(),
                feedback.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminFeedbackItemResponse> getAllFeedbacks() {
        return feedbackRepository.findAllFeedbacksForAdmin();
    }
}

