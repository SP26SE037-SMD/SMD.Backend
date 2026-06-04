package com.example.smd.repositories;

import com.example.smd.entities.GoogleFormRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleFormRecordRepository extends JpaRepository<GoogleFormRecord, UUID> {
    List<GoogleFormRecord> findByCurriculum_CurriculumId(UUID curriculumId);
    List<GoogleFormRecord> findByDepartment_DepartmentId(UUID departmentId);
    Optional<GoogleFormRecord> findByGoogleFormId(String googleFormId);
}
