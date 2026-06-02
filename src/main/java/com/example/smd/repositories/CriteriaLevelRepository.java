package com.example.smd.repositories;

import com.example.smd.entities.CriteriaLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CriteriaLevelRepository extends JpaRepository<CriteriaLevel, UUID> {
    List<CriteriaLevel> findByCriterion_CriterionId(UUID criterionId);
    void deleteByCriterion_CriterionId(UUID criterionId);
    boolean existsByCriterion_CriterionIdAndLevel_LevelId(UUID criterionId, UUID levelId);
}
