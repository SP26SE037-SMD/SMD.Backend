package com.example.smd.repositories;

import com.example.smd.entities.Regulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegulationRepository extends JpaRepository<Regulation, UUID>, JpaSpecificationExecutor<Regulation> {
    Optional<Regulation> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndRegulationIdNot(String code, UUID regulationId);

    Optional<Regulation> findByCodeAndMajor_MajorId(String code, UUID majorId);
}
