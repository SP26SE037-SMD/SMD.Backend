package com.example.smd.services;

import com.example.smd.dto.excel.SubjectImportDTO;
import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResult;
import com.example.smd.entities.Department;
import com.example.smd.entities.Subject;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PrerequisiteMapper;
import com.example.smd.mapper.SubjectMapper;
import com.example.smd.repositories.*;
import com.example.smd.services.excelService.ExcelImporter;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectService {

    SubjectRepository subjectRepository;
    DepartmentRepository departmentRepository;
    PrerequisiteRepository prerequisiteRepository;

    SubjectMapper subjectMapper;
    PrerequisiteMapper prerequisiteMapper;

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode()))
            throw new AppException(ErrorCode.SUBJECT_CODE_EXISTS);

        Subject subject = subjectMapper.toSubject(request);

        //Check Department có tồn tại hay không
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        subject.setDepartment(department);

        subject.setStatus(SubjectStatus.DRAFT.toString());
        subject = subjectRepository.save(subject);

        SubjectResponse response = subjectMapper.toSubjectResponse(subject);

        return response;
    }

    public Page<SubjectResponse> getAll(String search, String searchBy, String status, UUID departmentId, Pageable pageable) {
        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search Logic (Name or Code)
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                if ("name".equalsIgnoreCase(searchBy)) {
                    predicates.add(cb.like(cb.lower(root.get("subjectName")), searchPattern));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("subjectCode")), searchPattern));
                }
            }

            // 2. Filter by Status
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }

            // 3. Filter by Department ID (New)
            if (departmentId != null) {
                // Join với bảng Department và so khớp ID
                predicates.add(cb.equal(root.get("department").get("departmentId"), departmentId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return subjectRepository.findAll(spec, pageable).map(subject -> {
            SubjectResponse response = subjectMapper.toSubjectResponse(subject);

            // Bổ sung Prerequisites (Nên cân nhắc tối ưu N+1 ở đây sau này)
            List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subject.getSubjectId())
                    .stream()
                    .map(prerequisiteMapper::toResponse)
                    .toList();
            response.setPreRequisite(prerequisites);

            return response;
        });
    }

    @Transactional
    public SubjectResponse update(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subjectMapper.updateSubject(subject, request);

        //Check Department có tồn tại hay không
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        subject.setDepartment(department);

        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }

    public void delete(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subject.setStatus(SubjectStatus.ARCHIVED.toString()); // Soft delete
        subjectRepository.save(subject);
    }

    @Transactional
    public SubjectResponse getDetail(UUID id) {
        // Sử dụng method findDetailById đã có JOIN FETCH
        Subject subject = subjectRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectResponse response = subjectMapper.toSubjectResponse(subject);

        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(id)
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
        response.setPreRequisite(prerequisites);

        return response;
    }

    @Transactional
    public SubjectResponse publishSubject(UUID subjectId, String decisionNo) {
        // 1. Tìm môn học, nếu không thấy thì ném lỗi
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 2. Cập nhật các trường liên quan đến việc ban hành
        subject.setDecisionNo(decisionNo);       // Gán số quyết định ban hành
        subject.setIsApproved(true);             // Đánh dấu đã phê duyệt
        subject.setStatus(SubjectStatus.COMPLETED.toString());                 // Chuyển từ Draft (null) sang Active (true)
        subject.setApprovedDate(Instant.now());  // Lưu ngày phê duyệt (nếu có field này)

        // 3. Lưu và trả về response
        SubjectResponse response = subjectMapper.toSubjectResponse(subjectRepository.save(subject));

        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
        response.setPreRequisite(prerequisites);

        return response;
    }

    @Transactional
    public SubjectResponse updateSubjectStatus(UUID subjectId, String newStatus) {
        // 1. Tìm môn học, nếu không thấy thì ném lỗi
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectStatus status;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            status = SubjectStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_SUBJECT_STATUS);
        }

        // 2. Cập nhật các trường liên quan đến việc ban hành
        subject.setStatus(status.toString());

        // 3. Lưu và trả về response
        SubjectResponse response = subjectMapper.toSubjectResponse(subjectRepository.save(subject));

        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
        response.setPreRequisite(prerequisites);

        return response;
    }

    @Transactional
    public ImportSubjectResponse importSubjects(MultipartFile file) {
        List<ImportSubjectResult> details = new ArrayList<>();
        List<Subject> subjectsToSave = new ArrayList<>();
        Set<String> subjectCodesInFile = new HashSet<>();

        try {
            List<SubjectImportDTO> rows = ExcelImporter.importFromExcel(file, SubjectImportDTO.class);

            for (SubjectImportDTO row : rows) {
                String subjectCode = trim(row.getSubjectCode());
                try {
                    String subjectName = trim(row.getSubjectName());
                    String departmentCode = trim(row.getDepartmentCode());

                    if (subjectCode == null || subjectName == null || departmentCode == null) {
                        details.add(ImportSubjectResult.builder()
                                .subjectCode(subjectCode)
                                .status("FAILED")
                                .message("Missing required fields: subjectCode, subjectName, departmentCode")
                                .build());
                        continue;
                    }

                    Integer credits = parseInteger(row.getCredits(), "credits");
                    if (credits == null) {
                        details.add(ImportSubjectResult.builder()
                                .subjectCode(subjectCode)
                                .status("FAILED")
                                .message("Invalid credits")
                                .build());
                        continue;
                    }

                    if (!subjectCodesInFile.add(subjectCode.toUpperCase())) {
                        details.add(ImportSubjectResult.builder()
                                .subjectCode(subjectCode)
                                .status("FAILED")
                                .message("Duplicate subjectCode in file")
                                .build());
                        continue;
                    }

                    if (subjectRepository.existsBySubjectCode(subjectCode)) {
                        details.add(ImportSubjectResult.builder()
                                .subjectCode(subjectCode)
                                .status("FAILED")
                                .message("Subject code already exists")
                                .build());
                        continue;
                    }

                    Department department = departmentRepository.findByDepartmentCode(departmentCode)
                            .orElse(null);
                    if (department == null) {
                        details.add(ImportSubjectResult.builder()
                                .subjectCode(subjectCode)
                                .status("FAILED")
                                .message("Department code not found")
                                .build());
                        continue;
                    }

                    Subject subject = Subject.builder()
                            .subjectCode(subjectCode)
                            .subjectName(subjectName)
                            .credits(credits)
                            .degreeLevel(trim(row.getDegreeLevel()))
                            .timeAllocation(trim(row.getTimeAllocation()))
                            .description(trim(row.getDescription()))
                            .department(department)
                            .minToPass(parseInteger(row.getMinToPass(), "minToPass"))
                            .studentLimit(parseInteger(row.getStudentLimit(), "studentLimit"))
                            .studentTasks(trim(row.getStudentTasks()))
                            .scoringScale(parseInteger(row.getScoringScale(), "scoringScale"))
                            .status(SubjectStatus.DRAFT.toString())
                            .createdAt(Instant.now())
                            .build();

                    subjectsToSave.add(subject);
                    details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("SUCCESS")
                            .message("Created successfully")
                            .build());
                } catch (AppException ex) {
                    details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message(ex.getMessage())
                            .build());
                }
            }

            subjectRepository.saveAll(subjectsToSave);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import subject failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportSubjectResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .details(details)
                .build();
    }

    private Integer parseInteger(String raw, String fieldName) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_KEY, "Invalid number for field " + fieldName + ": " + value);
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public List<SubjectResponse> getSubjectsByDepartment(UUID departmentId) {
        // Fetch entities from DB
        List<Subject> subjects = subjectRepository.findAllByDepartmentId(departmentId);

        // Map to Response DTOs
        return subjects.stream()
                .map(subject -> {
                    SubjectResponse response = subjectMapper.toSubjectResponse(subject);

                    // 1. Bổ sung Prerequisites
                    List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subject.getSubjectId())
                            .stream()
                            .map(prerequisiteMapper::toResponse)
                            .toList();
                    response.setPreRequisite(prerequisites);

                    return response;
                })
                .toList();
    }
}
