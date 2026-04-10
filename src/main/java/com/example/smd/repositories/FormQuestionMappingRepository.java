package com.example.smd.repositories;

import com.example.smd.entities.FormQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormQuestionMappingRepository extends JpaRepository<FormQuestionMapping, UUID> {
    List<FormQuestionMapping> findByFormRecord_Id(UUID formRecordId);

    void deleteByFormRecord_Id(UUID formRecordId);
}
