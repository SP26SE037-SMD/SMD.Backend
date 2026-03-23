package com.example.smd.services;

import com.example.smd.dto.request.CloAssessmentMappingBatchRequest;
import com.example.smd.dto.request.CloAssessmentMappingRequest;
import com.example.smd.dto.response.clo.CloAssessmentMappingResponse;
import com.example.smd.entities.CLO_Assessment;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Assessment;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloAssessmentMappingMapper;
import com.example.smd.repositories.CLOsRepository;
import com.example.smd.repositories.AssessmentRepository;
import com.example.smd.repositories.CloAssessmentMappingRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloAssessmentMappingService {

    CloAssessmentMappingRepository repository;
    CloAssessmentMappingMapper mapper;
    CLOsRepository cloRepository;
    AssessmentRepository assessmentRepository;
    SyllabusRepository syllabusRepository;

    @Transactional
    public CloAssessmentMappingResponse createMapping(CloAssessmentMappingRequest request) {
        UUID cloId = parseUuid(request.getCloId());
        UUID assessmentId = parseUuid(request.getAssessmentId());

        if (repository.existsByClo_CloIdAndAssessment_AssessmentId(cloId, assessmentId)) {
            throw new AppException(ErrorCode.MAPPING_ALREADY_EXISTS);
        }

        CLOs clo = cloRepository.findById(cloId)
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        CLO_Assessment entity = CLO_Assessment.builder()
                .clo(clo)
                .assessment(assessment)
                .build();

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public List<CloAssessmentMappingResponse> createBatch(CloAssessmentMappingBatchRequest request) {
        return request.getMappings().stream()
                .map(this::createMapping)
                .toList();
    }

    @Transactional
    public List<CloAssessmentMappingResponse> getBySyllabus(String syllabusId) {
        UUID id = parseUuid(syllabusId);
        if (!syllabusRepository.existsById(id)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        return repository.findByAssessment_Syllabus_SyllabusId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloAssessmentMappingResponse> getByClo(String cloId) {
        UUID id = parseUuid(cloId);
        if (!cloRepository.existsById(id)) {
            throw new AppException(ErrorCode.CLO_NOT_FOUND);
        }

        return repository.findByClo_CloId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloAssessmentMappingResponse> getByAssessment(String assessmentId) {
        UUID id = parseUuid(assessmentId);
        if (!assessmentRepository.existsById(id)) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }

        return repository.findByAssessment_AssessmentId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMapping(String id) {
        UUID mappingId = parseUuid(id);
        if (!repository.existsById(mappingId)) {
            throw new AppException(ErrorCode.MAPPING_NOT_FOUND);
        }
        repository.deleteById(mappingId);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
