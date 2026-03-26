package com.example.smd.repositories;

import com.example.smd.entities.FeedbackAnswers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackAnswerRepository extends JpaRepository<FeedbackAnswers, UUID> {
    List<FeedbackAnswers> findByFeedbackSubmission_Id(UUID submissionId);
}
