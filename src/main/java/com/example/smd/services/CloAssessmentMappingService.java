package com.example.smd.services;

import com.example.smd.dto.request.CloAssessmentMappingBatchRequest;
import com.example.smd.dto.request.CloAssessmentMappingRequest;
import com.example.smd.dto.response.clo.CloAssessmentMappingResponse;
import com.example.smd.dto.response.validate.AssessmentCloMappingValidationResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloAssessmentMappingMapper;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloAssessmentMappingService {

    CloAssessmentMappingRepository repository;
    CloAssessmentMappingMapper mapper;
    CLOsRepository cloRepository;
    AssessmentRepository assessmentRepository;
    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    GeminiService geminiService;

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

    public AssessmentCloMappingValidationResult checkMapping(List<CloAssessmentMappingRequest> request, UUID syllabusId) {

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
        Subject subject = subjectRepository.findById(syllabus.getSubject().getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
        List<CLOs> cloList = cloRepository.findBySubject_SubjectId(subject.getSubjectId());
        List<Map<String, String>> cloJsonData = cloList.stream().map(clo -> {
            Map<String, String> map = new HashMap<>();
            map.put("clo_id", clo.getCloId().toString());
            map.put("clo_code", clo.getCloCode());
            map.put("description", clo.getDescription());
            return map;
        }).collect(Collectors.toList());

        List<Assessment> assessmentList = assessmentRepository.findBySyllabus_SyllabusId(syllabusId);

        List<Map<String, String>> assessmentJsonData = assessmentList.stream().map(a -> {
            Map<String, String> map = new HashMap<>();
            map.put("assessment_id", a.getAssessmentId().toString());
            map.put("question_type", a.getQuestionType()); // Type của câu hỏi
            map.put("knowledge_skill", a.getKnowledgeSkill()); // Kỹ năng cốt lõi
            map.put("grading_guide", a.getGradingGuide()); // Rubric/Hướng dẫn chấm
            return map;
        }).collect(Collectors.toList());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String currentMapping = buildAssessmentCloMappingForAI(request);
            String assessmentJsonString = objectMapper.writeValueAsString(assessmentJsonData);
            String cloJsonString = objectMapper.writeValueAsString(cloJsonData);

            return geminiService.checkAssessmentCloMapping(assessmentJsonString, cloJsonString, currentMapping);
        } catch (JsonProcessingException e) {
            // Bắn ra lỗi Runtime hoặc Custom Exception của hệ thống bác
            throw new RuntimeException("Lỗi khi parse đối tượng sang JSON String", e);
        }
    }

    public String buildAssessmentCloMappingForAI(List<CloAssessmentMappingRequest> requests) {

        // 1. LẤY ID TỪ REQUEST
        Set<UUID> assessmentIds = requests.stream()
                .map(req -> UUID.fromString(req.getAssessmentId()))
                .collect(Collectors.toSet());

        Set<UUID> cloIds = requests.stream()
                .map(req -> UUID.fromString(req.getCloId()))
                .collect(Collectors.toSet());

        // 2. GỌI DATABASE ĐỂ LẤY THỰC THỂ (Optional: Dùng hàm JOIN FETCH để tránh LazyLoad)
        List<Assessment> assessmentsFromDb = assessmentRepository.findAllById(assessmentIds);
        List<CLOs> closFromDb = cloRepository.findAllById(cloIds);

        // 3. TẠO MAP CHUYỂN ĐỔI: Chuyển ID (UUID) sang ID dạng String để đối chiếu
        Map<String, String> assessmentIdMap = assessmentsFromDb.stream()
                .collect(Collectors.toMap(a -> a.getAssessmentId().toString(), a -> a.getAssessmentId().toString()));

        Map<String, String> cloIdToCodeMap = closFromDb.stream()
                .collect(Collectors.toMap(clo -> clo.getCloId().toString(), CLOs::getCloCode));

        // 4. GOM NHÓM DỮ LIỆU: KEY = Assessment ID, VALUE = List<CLO Code>
        Map<String, List<String>> mappingResult = requests.stream()
                .filter(req -> assessmentIdMap.containsKey(req.getAssessmentId().toString())
                        && cloIdToCodeMap.containsKey(req.getCloId().toString()))
                .collect(Collectors.groupingBy(
                        req -> req.getAssessmentId().toString(),
                        Collectors.mapping(req -> cloIdToCodeMap.get(req.getCloId().toString()), Collectors.toList())
                ));

        // 5. PARSE SANG JSON STRING
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mappingResult);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse mapping data cho AI Prompt", e);
        }
    }
}
