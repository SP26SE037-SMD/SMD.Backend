package com.example.smd.repositories;

import com.example.smd.entities.FeedbackFormSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackFormSectionRepository extends JpaRepository<FeedbackFormSection, UUID> {
    List<FeedbackFormSection> findByFormRecord_IdOrderByOrderIndexAsc(UUID formId);

    @Query("SELECT MAX(s.orderIndex) FROM FeedbackFormSection s WHERE s.formRecord.id = :formId")
    Optional<Integer> findMaxOrderIndex(@Param("formId") UUID formId);
}
