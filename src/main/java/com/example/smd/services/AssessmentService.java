package com.example.smd.services;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.dto.response.validate.AssessmentValidationResult;
import com.example.smd.entities.*;
import com.example.smd.enums.*;
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

import java.util.*;
import java.util.stream.Collectors;

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
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!SyllabusStatus.PUBLISHED.toString().equals(status)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(status)) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) ||RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
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
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!MaterialStatus.PUBLISHED.toString().equalsIgnoreCase(assessment.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if ("DRAFT".equals(assessment.getStatus())|| MaterialStatus.REVISION_REQUESTED.toString().equals(assessment.getStatus())) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
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
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID syllabusId = request.getSyllabusId();
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
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
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
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

        if (!("DRAFT".equals(assessment.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(assessment.getStatus()))) {
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
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(assessment.getSyllabus().getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(assessment.getSyllabus().getStatus()))) {
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

    @jakarta.transaction.Transactional
    public void updateAssessmentStatusBySyllabus(String syllabusId, String newStatus) {
        // 1. Kiểm tra trạng thái hợp lệ từ Enum SyllabusStatus (hoặc MaterialStatus nếu bạn có riêng)
        SyllabusStatus status;
        try {
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_MATERIAL_STATUS);
        }

        UUID uuidSyllabusId = UUID.fromString(syllabusId);

        // 2. Kiểm tra Syllabus có tồn tại không trước khi update Material
        if (!syllabusRepository.existsById(uuidSyllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        // 3. Cập nhật hàng loạt trạng thái các Materials thuộc Syllabus này
        // Lưu ý: Material đi theo Syllabus nên ta dùng updateStatusBySyllabusId
        int affectedRows = assessmentRepository.updateStatusBySyllabusId(status.toString(), uuidSyllabusId);
    }

    public AssessmentValidationResult validate(List<AssessmentRequest> inputs) {
        AssessmentValidationResult result = new AssessmentValidationResult();

        // 1. ĐẾM SỐ LƯỢNG (Counting)
        long totalCount = inputs.size();

        // 1. Lấy tất cả categoryId mà Frontend gửi lên (Dùng Set để loại bỏ ID trùng lặp)
        Set<UUID> categoryIds = inputs.stream()
                .map(AssessmentRequest::getCategoryId)
                .collect(Collectors.toSet());

        // 2. Gọi Database ĐÚNG 1 LẦN để lấy thông tin các Category này
        // Giả sử bạn đã tiêm (inject) assessmentCategoryRepository vào Service
        List<Assessment_Category> categoriesFromDb = assessmentCategoryRepository.findAllById(categoryIds);

        // 3. Đưa vào Map<UUID, Tên Category> để tra cứu siêu tốc trong RAM
        Map<UUID, String> categoryNameMap = categoriesFromDb.stream()
                .collect(Collectors.toMap(Assessment_Category::getCategoryId, Assessment_Category::getCategoryName));

        // 4. Bây giờ thì đếm thoải mái dựa vào TÊN của Category
        // Dùng ignoreCase để phòng hờ DB lưu chữ HOA/thường khác nhau
        long finalCount = inputs.stream()
                .filter(a -> "Summative".equalsIgnoreCase(categoryNameMap.get(a.getCategoryId())))
                .count();

        long formativeCount = inputs.stream()
                .filter(a -> "Formative".equalsIgnoreCase(categoryNameMap.get(a.getCategoryId())))
                .count();

        // 3. LOGIC VALIDATE DỰA TRÊN SỐ LƯỢNG BÀI
        // Ràng buộc A: Không được có nhiều hơn 1 bài Cuối kỳ
        if (finalCount == 0) {
            result.addError("MISSING_FINAL_ASSESSMENT",
                    "Syllabus must include at least one Summative assessment (Final Exam, Project, etc.).");
        }

        if (finalCount > 2) {
            result.addError("MULTIPLE_FINAL_ASSESSMENTS",
                    "You cannot have more than 1 final assessment. Currently, there are " + finalCount + ".");
        }

        // Ràng buộc B: (Tùy chọn) Khống chế số lượng cột điểm tối đa (Ví dụ: Tối đa 6 bài)
        if (totalCount > 6) {
            result.addError("TOO_MANY_ASSESSMENTS",
                    "Syllabus can have a maximum of 6 assessments to avoid overloading students. You have created " + totalCount + ".");
        }

        // 1. Tính toán các thông số
        double totalWeight = inputs.stream()
                .mapToDouble(a -> a.getWeight() != null ? a.getWeight() : 0.0)
                .sum();

        // Ràng buộc C: (Tùy chọn) Phải có ít nhất 1 bài quá trình nếu môn học không phải là Đồ án 100%
        if (formativeCount == 0 && totalWeight < 100) {
            result.addError("MISSING_FORMATIVE_ASSESSMENT",
                    "Please add at least 1 formative assessment (e.g., QUIZ, ASSIGNMENT).");
        }

        result.setSummary(AssessmentValidationResult.AssessmentSummary.builder()
                .currentTotalWeight(totalWeight)
                .hasFinalAssessment(finalCount > 0)
                .hasFormativeAssessment(formativeCount > 0)
                .totalAssessmentCount(totalCount)
                .formativeCount(formativeCount)
                .finalCount(finalCount)
                .build()); // Lệnh build() sẽ đóng gói tất cả lại thành object

        // 3. Viết các câu IF bắt lỗi (Giống đoạn code tin nhắn trước)
        if (totalWeight < 100) {
            result.addError("WEIGHT_SHORTAGE",
                    "Total weight is short by " + (100 - totalWeight) + "%. It must be exactly 100%.");
        } else if (totalWeight > 100) {
            result.addError("WEIGHT_SURPLUS",
                    "Total weight exceeded by " + (totalWeight - 100) + "%. It must be exactly 100%.");
        }

        return result;
    }
}
