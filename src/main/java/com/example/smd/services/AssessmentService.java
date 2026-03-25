package com.example.smd.services;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.entities.Assessment;
import com.example.smd.entities.Assessment_Category;
import com.example.smd.entities.Assessment_Type;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AssessmentMapper;
import com.example.smd.repositories.AssessmentCategoryRepository;
import com.example.smd.repositories.AssessmentRepository;
import com.example.smd.repositories.AssessmentTypeRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {
    private final AccountService accountService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCategoryRepository assessmentCategoryRepository;
    private final AssessmentTypeRepository assessmentTypeRepository;
    private final SyllabusRepository syllabusRepository;
    private final AssessmentMapper assessmentMapper;

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> getAllAssessments(UUID syllabusId,
                                                      String status,
                                                      String search,
                                                      int page,
                                                      int size,
                                                      String[] sort,
                                                      String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            if (!status.equals(SyllabusStatus.PUBLISHED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (status.equals(PloStatus.DRAFT.toString())) {
            if (!(account.getRole().getRoleName().equals("PDCM") || account.getRole().getRoleName().equals("COLLABORATOR"))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        Specification<Assessment> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (syllabusId != null) {
                predicates.add(cb.equal(root.get("syllabus").get("syllabusId"), syllabusId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("questionType"), "")), searchPattern),
                        cb.like(cb.lower(cb.coalesce(root.get("knowledgeSkill"), "")), searchPattern),
                        cb.like(cb.lower(cb.coalesce(root.get("completionCriteria"), "")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return assessmentRepository.findAll(specification, pagingSort)
                .map(assessmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentById(UUID assessmentId, String accountId) {
        Assessment assessment =
                assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // 3. Logic Phân quyền:
        // Nếu là STUDENT hoặc LECTURER, chỉ cho phép xem nếu status là PUBLISHED
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            if (!"PUBLISHED".equalsIgnoreCase(assessment.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (assessment.getStatus().equals("DRAFT")|| assessment.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString())) {
            if (!(account.getRole().getRoleName().equals("PDCM") || account.getRole().getRoleName().equals("COLLABORATOR"))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        return assessmentMapper.toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsBySyllabus(UUID syllabusId) {
        return assessmentRepository.findBySyllabus_SyllabusIdOrderByPartAsc(syllabusId)
                .stream()
                .map(assessmentMapper::toResponse)
                .toList();
    }

    @Transactional
    public AssessmentResponse createAssessment(AssessmentRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID syllabusId = request.getSyllabusId();
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(syllabus.getStatus().equals("DRAFT") || syllabus.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.ASSESSMENT_CANNOT_CREATE);
        }

        Assessment_Category category =
            assessmentCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NOT_FOUND));
        Assessment_Type type =
            assessmentTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NOT_FOUND));

        validateWeightInput(request.getWeight());
        validateSyllabusWeightLimit(syllabusId, request.getWeight(), null);

        Assessment assessment = assessmentMapper.toEntity(request);
        assessment.setAssessmentCategory(category);
        assessment.setAssessmentType(type);
        assessment.setSyllabus(syllabus);
        assessment.setStatus(normalizeStatus(request.getStatus()));

        assessment = assessmentRepository.save(assessment);
        return assessmentMapper.toResponse(assessment);
    }

    @Transactional
    public AssessmentResponse updateAssessment(UUID assessmentId,
                                               AssessmentRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        Assessment_Category category =
            assessmentCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NOT_FOUND));
        Assessment_Type type =
            assessmentTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NOT_FOUND));

        if (!(assessment.getStatus().equals("DRAFT") || assessment.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }

        UUID syllabusId = request.getSyllabusId();
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        validateWeightInput(request.getWeight());
        validateSyllabusWeightLimit(syllabusId, request.getWeight(), assessmentId);

        assessmentMapper.updateEntity(assessment, request);
        assessment.setAssessmentCategory(category);
        assessment.setAssessmentType(type);
        assessment.setSyllabus(syllabus);
        assessment.setStatus(normalizeStatus(request.getStatus()));

        assessment = assessmentRepository.save(assessment);
        return assessmentMapper.toResponse(assessment);
    }

    @Transactional
    public AssessmentResponse updateAssessmentStatus(UUID assessmentId, String status) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        if (status == null || status.trim().isEmpty()) {
            throw new AppException(ErrorCode.ASSESSMENT_STATUS_REQUIRED);
        }

        assessment.setStatus(status.trim().toUpperCase());
        assessment = assessmentRepository.save(assessment);
        return assessmentMapper.toResponse(assessment);
    }

    @Transactional
    public boolean deleteAssessment(UUID assessmentId, String accountId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (!(assessment.getSyllabus().getStatus().equals("DRAFT") || assessment.getSyllabus().getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }

        if(assessment.getStatus().equals("DRAFT")){
            assessmentRepository.delete(assessment);
            return true;
        }
        assessment.setStatus("ARCHIVED");
        assessmentRepository.save(assessment);
        return true;
    }

    private void validateWeightInput(Double weight) {
        if (weight == null || weight <= 0 || weight > 100) {
            throw new AppException(ErrorCode.ASSESSMENT_WEIGHT_INVALID);
        }
    }

    private void validateSyllabusWeightLimit(UUID syllabusId,
                                             Double incomingWeight,
                                             UUID assessmentId) {
        Double currentTotal = assessmentId == null
                ? assessmentRepository.sumWeightBySyllabusId(syllabusId)
                : assessmentRepository.sumWeightBySyllabusIdAndAssessmentIdNot(syllabusId, assessmentId);

        if (currentTotal + incomingWeight > 100.0) {
            throw new AppException(ErrorCode.ASSESSMENT_WEIGHT_EXCEEDS_LIMIT);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "DRAFT";
        }
        return status.trim().toUpperCase();
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
