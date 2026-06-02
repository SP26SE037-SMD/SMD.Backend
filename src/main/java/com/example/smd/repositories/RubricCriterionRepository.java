package com.example.smd.repositories;

import com.example.smd.entities.RubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {
    List<RubricCriterion> findByRubric_RubricIdOrderByDisplayOrderAsc(UUID rubricId);
    void deleteByRubric_RubricId(UUID rubricId);
}
