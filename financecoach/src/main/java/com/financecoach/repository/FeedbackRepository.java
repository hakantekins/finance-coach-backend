package com.financecoach.repository;

import com.financecoach.dto.response.AdminFeedbackItemResponse;
import com.financecoach.model.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("""
        SELECT new com.financecoach.dto.response.AdminFeedbackItemResponse(
            f.id,
            u.fullName,
            f.message,
            f.createdAt
        )
        FROM Feedback f
        JOIN User u ON u.id = f.userId
        ORDER BY f.createdAt DESC
    """)
    List<AdminFeedbackItemResponse> findAllFeedbacksForAdmin();
}

