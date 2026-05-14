package com.example.smd.services;

import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.entities.Department;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Assessment;
import com.example.smd.entities.Session;
import com.example.smd.entities.Material;
import com.example.smd.entities.Blocks;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.RoleName;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SyllabusMapper;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.repositories.AssessmentRepository;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.MaterialRepository;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.SyllabusSourceRepository;
import com.example.smd.mapper.AssessmentMapper;
import com.example.smd.mapper.SessionMapper;
import com.example.smd.mapper.MaterialMapper;
import com.example.smd.mapper.BlockMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import static org.apache.logging.log4j.ThreadContext.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SyllabusService {
    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    SyllabusMapper syllabusMapper;
    AccountService accountService;
    DepartmentService departmentService;
    AssessmentRepository assessmentRepository;
    SessionRepository sessionRepository;
    MaterialRepository materialRepository;
    BlockRepository blockRepository;
    AssessmentMapper assessmentMapper;
    SessionMapper sessionMapper;
    MaterialMapper materialMapper;
    BlockMapper blockMapper;
    SyllabusSourceRepository syllabusSourceRepository;

    // 1. Tạo mới
    @Transactional
    public SyllabusResponse create(SyllabusRequest request, String accountId) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!(SubjectStatus.WAITING_SYLLABUS.toString().equals(subject.getStatus()) || SubjectStatus.COMPLETED.toString().equals(subject.getStatus()))) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_CREATE);
        }

        Syllabus syllabus = syllabusMapper.toSyllabus(request);
        syllabus.setSubject(subject);
        syllabus.setStatus("DRAFT");

        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    // 2. Cập nhật thông tin chung
    @Transactional
    public SyllabusResponse update(UUID id, SyllabusRequest request, String accountId) {
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (!(SyllabusStatus.DRAFT.toString().equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_EDITABLE);
        }

        syllabusMapper.updateSyllabus(syllabus, request);
        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    // 3. Cập nhật Status
    @Transactional
    public SyllabusResponse updateStatus(UUID id, String newStatus) {
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        SyllabusStatus status;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_SYLLABUS_STATUS);
        }

        syllabus.setStatus(status.toString());
        if (SyllabusStatus.APPROVED.toString().equalsIgnoreCase(newStatus)) {
            syllabus.setApprovedDate(Instant.now());
        }
        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    // 4. Xóa đệm (Soft Delete)
    @Transactional
    public void delete(UUID id, String accountId) {
        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // Kiểm tra xem Syllabus có tồn tại không
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if ("DRAFT".equals(syllabus.getStatus())) {
            // Xóa các bảng con trước để tránh FK constraint violation
            assessmentRepository.deleteAll(assessmentRepository.findBySyllabus_SyllabusId(id));
            sessionRepository.deleteAll(sessionRepository.findBySyllabus_SyllabusId(id));
            // materials đã có CascadeType.ALL trên Syllabus entity, sẽ tự xóa
            // nhưng syllabus_sources không có cascade → xóa thủ công
            syllabusSourceRepository.deleteAll(syllabusSourceRepository.findBySyllabus_SyllabusId(id));
            syllabusRepository.delete(syllabus);
        } else {
            syllabus.setStatus("ARCHIVED");
            syllabusRepository.save(syllabus);
        }
    }

    // 5. Get All by Subject
    @Transactional
    public List<SyllabusResponse> getAllBySubject(UUID subjectId, String status, String accountId) {
        // 1. Lấy thông tin Account & Role
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // Chuẩn hóa status đầu vào
        String finalStatus = (status == null || status.trim().isEmpty()) ? null : status.trim();

        // 2. Phân quyền: Student/Lecturer chỉ được xem PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (finalStatus != null && !finalStatus.isBlank() && !SyllabusStatus.PUBLISHED.name().equals(finalStatus)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(finalStatus)) {
            if (!RoleName.HOPDC.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        List<Syllabus> syllabuses;

        // 3. Truy vấn dựa trên bộ lọc cuối cùng
        if (finalStatus == null) {
            // Tìm theo Subject ID và Status (AND)
            if(RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)){
                syllabuses = syllabusRepository.findActiveAndArchivedSyllabus(subjectId);
            } else {
                syllabuses = syllabusRepository.findBySubject_SubjectId(subjectId);
            }
        } else {
            syllabuses = syllabusRepository.findBySubject_SubjectIdAndStatus(subjectId, finalStatus);
        }

        return syllabuses.stream()
                .map(syllabusMapper::toResponse)
                .toList();
    }

    // 6. Get Detail
    @Transactional
    public SyllabusResponse getDetail(UUID id, String accountId) {
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName()) || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!PloStatus.PUBLISHED.toString().equals(syllabus.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (SyllabusStatus.DRAFT.toString().equals(syllabus.getStatus())) {
            if (!RoleName.HOPDC.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return syllabusRepository.findById(id)
                .map(syllabusMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
    }

    public List<SyllabusResponse> getSyllabusesByDepartment(String accountId, String status) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID departmentUuid = UUID.fromString(account.getDepartmentId());
        String newStatus = "";
        if (SyllabusStatus.PENDING_REVIEW.toString().equals(status)) {
            newStatus = SyllabusStatus.PENDING_REVIEW.toString();
        } else if (SyllabusStatus.IN_PROGRESS.toString().equals(status)) {
            newStatus = SyllabusStatus.IN_PROGRESS.toString();
        }

        List<Syllabus> syllabuses = syllabusRepository.findByDepartmentAndStatus(
                departmentUuid,
                newStatus
        );

        // 3. Map sang Response DTO
        return syllabuses.stream()
                .map(syllabusMapper::toResponse)
                .toList();
    }

    @Transactional
    public void copySyllabusData(UUID oldSyllabusId, UUID newSyllabusId) {
        Syllabus newSyllabus = syllabusRepository.findById(newSyllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        // 1. Copy Assessment
        List<Assessment> oldAssessments = assessmentRepository.findBySyllabus_SyllabusId(oldSyllabusId);
        List<Assessment> newAssessments = oldAssessments.stream().map(old -> {
            Assessment newAssessment = assessmentMapper.cloneEntity(old);
            newAssessment.setSyllabus(newSyllabus);
            return newAssessment;
        }).toList();
        assessmentRepository.saveAll(newAssessments);

        // 2. Copy Session
        List<Session> oldSessions = sessionRepository.findBySyllabus_SyllabusId(oldSyllabusId);
        List<Session> newSessions = oldSessions.stream().map(old -> {
            Session newSession = sessionMapper.cloneEntity(old);
            newSession.setSyllabus(newSyllabus);
            return newSession;
        }).toList();
        sessionRepository.saveAll(newSessions);

        // 3. Copy Material & Blocks
        List<Material> oldMaterials = materialRepository.findBySyllabus_SyllabusId(oldSyllabusId);
        for (Material oldMaterial : oldMaterials) {
            Material newMaterial = materialMapper.cloneMaterial(oldMaterial);
            newMaterial.setSyllabus(newSyllabus);
            newMaterial = materialRepository.save(newMaterial);

            List<Blocks> oldBlocks = blockRepository.findAllByMaterial_MaterialIdOrderByIdxAsc(oldMaterial.getMaterialId());
            Material finalNewMaterial = newMaterial;
            List<Blocks> newBlocks = oldBlocks.stream().map(old -> {
                Blocks newBlock = blockMapper.cloneBlock(old);
                newBlock.setMaterial(finalNewMaterial);
                return newBlock;
            }).toList();
            blockRepository.saveAll(newBlocks);
        }
    }
}
