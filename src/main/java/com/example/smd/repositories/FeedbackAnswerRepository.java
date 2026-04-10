package com.example.smd.repositories;

import com.example.smd.entities.FeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackAnswerRepository extends JpaRepository<FeedbackAnswer, UUID> {
    List<FeedbackAnswer> findBySubmission_Id(UUID submissionId);
}
