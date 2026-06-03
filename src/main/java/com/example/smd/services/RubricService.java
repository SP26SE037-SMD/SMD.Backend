package com.example.smd.services;

import com.example.smd.dto.request.rubric.*;
import com.example.smd.dto.response.rubric.*;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RubricService {

    RubricRepository rubricRepository;
    RubricCriterionRepository rubricCriterionRepository;
    RubricLevelRepository rubricLevelRepository;
    CriteriaLevelRepository criteriaLevelRepository;
    SyllabusRepository syllabusRepository;
    ObjectMapper objectMapper;

    // ===================== RUBRIC CRUD =====================

    @Transactional
    public RubricResponse createRubric(RubricRequest request) {
        UUID syllabusId = UUID.fromString(request.getSyllabusId());
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Syllabus not found"));

        if (rubricRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric code already exists: " + request.getCode());
        }

        Rubric rubric = Rubric.builder()
                .syllabus(syllabus)
                .code(request.getCode())
                .name(request.getName())
                .build();
        rubric = rubricRepository.save(rubric);

        if (request.getCriteria() != null) {
            saveCriteria(rubric, request.getCriteria());
        }

        return toRubricResponse(rubricRepository.findById(rubric.getRubricId()).orElseThrow());
    }

    @Transactional
    public RubricResponse getRubricById(String id) {
        Rubric rubric = rubricRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric not found"));
        return toRubricResponse(rubric);
    }

    @Transactional
    public List<RubricResponse> getAllRubrics() {
        return rubricRepository.findAll().stream()
                .map(this::toRubricResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RubricResponse> getRubricsBySyllabusId(String syllabusId) {
        UUID id = UUID.fromString(syllabusId);
        if (!syllabusRepository.existsById(id)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Syllabus not found");
        }
        return rubricRepository.findBySyllabus_SyllabusId(id).stream()
                .map(this::toRubricResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String getRubricsBySyllabusIdAsText(String syllabusId) {
        List<RubricResponse> rubrics = getRubricsBySyllabusId(syllabusId);

        if (rubrics == null || rubrics.isEmpty()) {
            return "Không có dữ liệu Rubric cho môn học này.";
        }

        StringBuilder rubricContent = new StringBuilder();
        for (RubricResponse rubric : rubrics) {
            // Nén thông tin Rubric
            rubricContent.append(String.format("📌 Rubric: %s (Mã: %s)\n", rubric.getName(), rubric.getCode()));

            if (rubric.getCriteria() != null) {
                for (CriterionResponse criteria : rubric.getCriteria()) {
                    // Nén thông tin Tiêu chí (Criterion)
                    // Dùng String.valueOf để tránh lỗi format nếu weight là kiểu Double/Float
                    rubricContent.append(String.format("   ▪ Tiêu chí: %s (Trọng số: %s%%)\n",
                            criteria.getName(),
                            String.valueOf(criteria.getWeight())));

                    if (criteria.getLevels() != null) {
                        for (CriteriaLevelResponse level : criteria.getLevels()) {
                            // Nén thông tin Mức độ (Level)
                            rubricContent.append(String.format("      - Mức %s: %s\n",
                                    level.getCode(),
                                    level.getDescription()));
                        }
                    }
                }
            }
            rubricContent.append("\n"); // Cách dòng giữa các Rubric để AI dễ nhìn
        }

        // Trả về chuỗi văn bản thuần túy (Plain text)
        return rubricContent.toString().trim();
    }

    @Transactional
    public RubricResponse updateRubric(String id, RubricRequest request) {
        Rubric rubric = rubricRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric not found"));

        if (!rubric.getCode().equals(request.getCode()) && rubricRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric code already exists: " + request.getCode());
        }

        if (request.getSyllabusId() != null) {
            UUID syllabusId = UUID.fromString(request.getSyllabusId());
            Syllabus syllabus = syllabusRepository.findById(syllabusId)
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Syllabus not found"));
            rubric.setSyllabus(syllabus);
        }

        rubric.setCode(request.getCode());
        rubric.setName(request.getName());
        rubric = rubricRepository.save(rubric);

        if (request.getCriteria() != null) {
            // Xoá toàn bộ criteria cũ (cascade xoá criteria_level)
            List<RubricCriterion> oldCriteria = rubricCriterionRepository.findByRubric_RubricIdOrderByDisplayOrderAsc(rubric.getRubricId());
            for (RubricCriterion criterion : oldCriteria) {
                criteriaLevelRepository.deleteByCriterion_CriterionId(criterion.getCriterionId());
            }
            rubricCriterionRepository.deleteByRubric_RubricId(rubric.getRubricId());
            saveCriteria(rubric, request.getCriteria());
        }

        return toRubricResponse(rubricRepository.findById(rubric.getRubricId()).orElseThrow());
    }

    @Transactional
    public void deleteRubric(String id) {
        Rubric rubric = rubricRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric not found"));

        List<RubricCriterion> criteria = rubricCriterionRepository.findByRubric_RubricIdOrderByDisplayOrderAsc(rubric.getRubricId());
        for (RubricCriterion criterion : criteria) {
            criteriaLevelRepository.deleteByCriterion_CriterionId(criterion.getCriterionId());
        }
        rubricCriterionRepository.deleteByRubric_RubricId(rubric.getRubricId());
        rubricRepository.delete(rubric);
    }

    // ===================== RUBRIC CRITERION CRUD =====================

    @Transactional
    public CriterionResponse createCriterion(String rubricId, CriterionRequest request) {
        Rubric rubric = rubricRepository.findById(UUID.fromString(rubricId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Rubric not found"));

        RubricCriterion criterion = RubricCriterion.builder()
                .rubric(rubric)
                .code(request.getCode())
                .criterionName(request.getCriterionName())
                .weight(request.getWeight())
                .displayOrder(request.getDisplayOrder())
                .build();
        criterion = rubricCriterionRepository.save(criterion);

        if (request.getLevels() != null) {
            saveCriteriaLevels(criterion, request.getLevels());
        }
        return toCriterionResponse(rubricCriterionRepository.findById(criterion.getCriterionId()).orElseThrow());
    }

    @Transactional
    public CriterionResponse getCriterionById(String criterionId) {
        RubricCriterion criterion = rubricCriterionRepository.findById(UUID.fromString(criterionId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Criterion not found"));
        return toCriterionResponse(criterion);
    }

    @Transactional
    public CriterionResponse updateCriterion(String criterionId, CriterionRequest request) {
        RubricCriterion criterion = rubricCriterionRepository.findById(UUID.fromString(criterionId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Criterion not found"));

        criterion.setCode(request.getCode());
        criterion.setCriterionName(request.getCriterionName());
        criterion.setWeight(request.getWeight());
        criterion.setDisplayOrder(request.getDisplayOrder());
        criterion = rubricCriterionRepository.save(criterion);

        if (request.getLevels() != null) {
            criteriaLevelRepository.deleteByCriterion_CriterionId(criterion.getCriterionId());
            saveCriteriaLevels(criterion, request.getLevels());
        }
        return toCriterionResponse(rubricCriterionRepository.findById(criterion.getCriterionId()).orElseThrow());
    }

    @Transactional
    public void deleteCriterion(String criterionId) {
        RubricCriterion criterion = rubricCriterionRepository.findById(UUID.fromString(criterionId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Criterion not found"));
        criteriaLevelRepository.deleteByCriterion_CriterionId(criterion.getCriterionId());
        rubricCriterionRepository.delete(criterion);
    }

    // ===================== RUBRIC LEVEL CRUD =====================

    @Transactional
    public RubricLevelResponse createLevel(RubricLevelRequest request) {
        RubricLevel level = RubricLevel.builder()
                .levelCode(request.getLevelCode())
                .minScore(request.getMinScore())
                .maxScore(request.getMaxScore())
                .displayOrder(request.getDisplayOrder())
                .build();
        level = rubricLevelRepository.save(level);
        return toRubricLevelResponse(level);
    }

    public RubricLevelResponse getLevelById(String levelId) {
        RubricLevel level = rubricLevelRepository.findById(UUID.fromString(levelId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "RubricLevel not found"));
        return toRubricLevelResponse(level);
    }

    public List<RubricLevelResponse> getAllLevels() {
        return rubricLevelRepository.findAll().stream()
                .map(this::toRubricLevelResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RubricLevelResponse updateLevel(String levelId, RubricLevelRequest request) {
        RubricLevel level = rubricLevelRepository.findById(UUID.fromString(levelId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "RubricLevel not found"));
        level.setLevelCode(request.getLevelCode());
        level.setMinScore(request.getMinScore());
        level.setMaxScore(request.getMaxScore());
        level.setDisplayOrder(request.getDisplayOrder());
        return toRubricLevelResponse(rubricLevelRepository.save(level));
    }

    @Transactional
    public void deleteLevel(String levelId) {
        RubricLevel level = rubricLevelRepository.findById(UUID.fromString(levelId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "RubricLevel not found"));
        rubricLevelRepository.delete(level);
    }

    // ===================== CRITERIA LEVEL CRUD =====================

    @Transactional
    public CriteriaLevelResponse createCriteriaLevel(String criterionId, CriteriaLevelRequest request) {
        RubricCriterion criterion = rubricCriterionRepository.findById(UUID.fromString(criterionId))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Criterion not found"));

        RubricLevel level = rubricLevelRepository.findByLevelCode(request.getLevelCode())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "RubricLevel with code '" + request.getLevelCode() + "' not found"));

        CriteriaLevel criteriaLevel = CriteriaLevel.builder()
                .criterion(criterion)
                .level(level)
                .description(request.getDescription())
                .build();
        criteriaLevel = criteriaLevelRepository.save(criteriaLevel);
        return toCriteriaLevelResponse(criteriaLevel);
    }

    @Transactional
    public CriteriaLevelResponse getCriteriaLevelById(String id) {
        CriteriaLevel criteriaLevel = criteriaLevelRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "CriteriaLevel not found"));
        return toCriteriaLevelResponse(criteriaLevel);
    }

    @Transactional
    public CriteriaLevelResponse updateCriteriaLevel(String id, CriteriaLevelRequest request) {
        CriteriaLevel criteriaLevel = criteriaLevelRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "CriteriaLevel not found"));

        RubricLevel level = rubricLevelRepository.findByLevelCode(request.getLevelCode())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "RubricLevel with code '" + request.getLevelCode() + "' not found"));

        criteriaLevel.setLevel(level);
        criteriaLevel.setDescription(request.getDescription());
        return toCriteriaLevelResponse(criteriaLevelRepository.save(criteriaLevel));
    }

    @Transactional
    public void deleteCriteriaLevel(String id) {
        CriteriaLevel criteriaLevel = criteriaLevelRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "CriteriaLevel not found"));
        criteriaLevelRepository.delete(criteriaLevel);
    }

    // ===================== PRIVATE HELPERS =====================

    private void saveCriteria(Rubric rubric, List<CriterionRequest> criterionRequests) {
        for (int i = 0; i < criterionRequests.size(); i++) {
            CriterionRequest req = criterionRequests.get(i);
            RubricCriterion criterion = RubricCriterion.builder()
                    .rubric(rubric)
                    .code(req.getCode())
                    .criterionName(req.getCriterionName())
                    .weight(req.getWeight())
                    .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : i + 1)
                    .build();
            criterion = rubricCriterionRepository.save(criterion);

            if (req.getLevels() != null) {
                saveCriteriaLevels(criterion, req.getLevels());
            }
        }
    }

    private void saveCriteriaLevels(RubricCriterion criterion, List<CriteriaLevelRequest> levelRequests) {
        for (CriteriaLevelRequest levelReq : levelRequests) {
            RubricLevel level = rubricLevelRepository.findByLevelCode(levelReq.getLevelCode())
                    .orElseGet(() -> rubricLevelRepository.save(
                            RubricLevel.builder()
                                    .levelCode(levelReq.getLevelCode())
                                    .build()
                    ));
            CriteriaLevel criteriaLevel = CriteriaLevel.builder()
                    .criterion(criterion)
                    .level(level)
                    .description(levelReq.getDescription())
                    .build();
            criteriaLevelRepository.save(criteriaLevel);
        }
    }

    private RubricResponse toRubricResponse(Rubric rubric) {
        List<RubricCriterion> criteria = rubricCriterionRepository
                .findByRubric_RubricIdOrderByDisplayOrderAsc(rubric.getRubricId());

        List<CriterionResponse> criterionResponses = criteria.stream()
                .map(this::toCriterionResponse)
                .collect(Collectors.toList());

        return RubricResponse.builder()
                .code(rubric.getCode())
                .name(rubric.getName())
                .syllabusId(rubric.getSyllabus() != null ? rubric.getSyllabus().getSyllabusId().toString() : null)
                .criteria(criterionResponses)
                .build();
    }

    private CriterionResponse toCriterionResponse(RubricCriterion criterion) {
        List<CriteriaLevel> criteriaLevels = criteriaLevelRepository
                .findByCriterion_CriterionId(criterion.getCriterionId());

        List<CriteriaLevelResponse> levelResponses = criteriaLevels.stream()
                .map(this::toCriteriaLevelResponse)
                .collect(Collectors.toList());

        return CriterionResponse.builder()
                .code(criterion.getCode())
                .name(criterion.getCriterionName())
                .weight(criterion.getWeight())
                .levels(levelResponses)
                .build();
    }

    private CriteriaLevelResponse toCriteriaLevelResponse(CriteriaLevel criteriaLevel) {
        return CriteriaLevelResponse.builder()
                .code(criteriaLevel.getLevel() != null ? criteriaLevel.getLevel().getLevelCode() : null)
                .description(criteriaLevel.getDescription())
                .build();
    }

    private RubricLevelResponse toRubricLevelResponse(RubricLevel level) {
        return RubricLevelResponse.builder()
                .levelId(level.getLevelId())
                .levelCode(level.getLevelCode())
                .minScore(level.getMinScore())
                .maxScore(level.getMaxScore())
                .displayOrder(level.getDisplayOrder())
                .build();
    }
}
