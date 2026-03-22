package com.example.smd.services;

import com.example.smd.dto.excel.SubjectImportDTO;
import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResult;
import com.example.smd.entities.Department;
import com.example.smd.entities.Subject;
import com.example.smd.enums.PloStatus;
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
    AccountService accountService;
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

    public Page<SubjectResponse> getAll(String search, String searchBy, String status, UUID departmentId, Pageable pageable, String accountId) {

        // 1. Lấy Account và xử lý phân quyền NGAY TẠI ĐẦU HÀM (Ngoài Specification)
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // Chuẩn hóa status
        String finalStatus = (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("all"))
                ? null : status.trim().toUpperCase();

        // Ép buộc Role thấp chỉ được xem PUBLISHED
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            finalStatus = "COMPLETED";
        }

        // Biến finalStatus này sẽ được dùng trong closure của Specification
        final String effectiveStatus = finalStatus;

        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // A. Search Logic (Code hoặc Name)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                if ("name".equalsIgnoreCase(searchBy)) {
                    predicates.add(cb.like(cb.lower(root.get("subjectName")), searchPattern));
                } else if ("code".equalsIgnoreCase(searchBy)) {
                    predicates.add(cb.like(cb.lower(root.get("subjectCode")), searchPattern));
                } else {
                    // Mặc định search cả 2 nếu searchBy không xác định
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("subjectName")), searchPattern),
                            cb.like(cb.lower(root.get("subjectCode")), searchPattern)
                    ));
                }
            }

            // B. Filter by Status (Sử dụng biến effectiveStatus đã xử lý phân quyền)
            if (effectiveStatus != null) {
                predicates.add(cb.equal(root.get("status"), effectiveStatus));
            }

            // C. Filter by Department ID
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("departmentId"), departmentId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. Query và Map sang Response
        return subjectRepository.findAll(spec, pageable).map(subject -> {
            SubjectResponse response = subjectMapper.toSubjectResponse(subject);

            // 3. Xử lý Prerequisites (Vẫn dùng repository hiện tại của bạn)
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
        //Check Department có tồn tại hay không
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!subject.getStatus().equals(SubjectStatus.DRAFT.toString())) {
            throw new AppException(ErrorCode.SUBJECT_NOT_DRAFT);
        }

        subject.setDepartment(department);
        subjectMapper.updateSubject(subject, request);
        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }

    public void delete(UUID id) {
        try {
            Subject subject = subjectRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

            if (subject.getStatus().equals(SubjectStatus.DRAFT.toString())) {
                subjectRepository.delete(subject);
            } else {
                subject.setStatus(SubjectStatus.ARCHIVED.toString());
                subjectRepository.save(subject);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public SubjectResponse getDetail(UUID id, String accountId) {
        // Sử dụng method findDetailById đã có JOIN FETCH
        Subject subject = subjectRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (account.getRole().getRoleName().equals("STUDENT") || account.getRole().getRoleName().equals("LECTURER")) {
            if (!subject.getStatus().equals(SubjectStatus.COMPLETED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (subject.getStatus().equals(SubjectStatus.DRAFT.toString())) {
            if (!account.getRole().getRoleName().equals("HOCFDC")) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        SubjectResponse response = subjectMapper.toSubjectResponse(subject);

        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(id)
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
        response.setPreRequisite(prerequisites);

        return response;
    }

    @Transactional
    public SubjectResponse getDetailByCode(String code, String accountId) {
        // 1. Tìm subject theo Code
        Subject subject = subjectRepository.findBySubjectCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 2. Lấy thông tin Account để phân quyền
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        String status = subject.getStatus();

        // Phân quyền: Student + Lecturer chỉ xem được COMPLETED
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            if (!status.equals(SubjectStatus.COMPLETED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // Phân quyền: DRAFT chỉ dành cho HOCFDC
        if (status.equals(SubjectStatus.DRAFT.toString())) {
            if (!roleName.equals("HOCFDC")) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // 3. Map sang Response DTO
        SubjectResponse response = subjectMapper.toSubjectResponse(subject);

        // 4. Lấy danh sách môn tiên quyết (Prerequisites)
        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subject.getSubjectId())
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
    public List<SubjectResponse> getSubjectsByDepartment(UUID departmentId, String accountId) {
        // 1. Lấy thông tin Account và Role
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        List<Subject> subjects;

        // 2. Phân quyền truy vấn
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            // Chỉ lấy những môn đã COMPLETED cho học sinh/giáo viên
            subjects = subjectRepository.findAllByDepartment_DepartmentIdAndStatus(departmentId, SubjectStatus.COMPLETED.toString());
        } else {
            // Các Role khác (HOCFDC, VP, ADMIN) lấy toàn bộ môn của phòng ban đó
            subjects = subjectRepository.findAllByDepartment_DepartmentId(departmentId);
        }

        // 3. Map to Response DTOs và bổ sung Prerequisites
        return subjects.stream()
                .map(subject -> {
                    SubjectResponse response = subjectMapper.toSubjectResponse(subject);

                    // Lấy môn tiên quyết
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
