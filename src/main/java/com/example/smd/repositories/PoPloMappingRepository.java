package com.example.smd.repositories;

import com.example.smd.entities.PO_PLO_Mapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PoPloMappingRepository extends JpaRepository<PO_PLO_Mapping, UUID> {

    boolean existsByPo_PoIdAndPlo_PloId(UUID poId, UUID ploId);

    long deleteByPo_PoIdAndPlo_PloId(UUID poId, UUID ploId);

    @EntityGraph(attributePaths = {"po", "plo"})
    List<PO_PLO_Mapping> findByPo_PoId(UUID poId);

    @EntityGraph(attributePaths = {"po", "plo"})
    List<PO_PLO_Mapping> findByPlo_PloId(UUID ploId);

    // Tìm mapping cho toàn bộ PLOs thuộc một Curriculum
    @EntityGraph(attributePaths = {"po", "plo"})
    List<PO_PLO_Mapping> findByPlo_Curriculum_CurriculumId(UUID curriculumId);
}
