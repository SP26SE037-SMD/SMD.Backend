package com.example.smd.repositories;

import com.example.smd.entities.FeedbackFormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackFormQuestionRepository extends JpaRepository<FeedbackFormQuestion, UUID> {
    List<FeedbackFormQuestion> findBySection_SectionIdOrderByOrderIndexAsc(UUID sectionId);

    @Query("SELECT MAX(q.orderIndex) FROM FeedbackFormQuestion q WHERE q.section.sectionId = :sectionId")
    Optional<Integer> findMaxOrderIndex(@Param("sectionId") UUID sectionId);
}
