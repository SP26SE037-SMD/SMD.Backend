package com.example.smd.services;

import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SyllabusMapper;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SyllabusService {
    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    SyllabusMapper syllabusMapper;
    AccountService accountService;

    // 1. Tạo mới
    @Transactional
    public SyllabusResponse create(SyllabusRequest request, String accountId) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!"HOPDC".equals(roleName)) {
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
        if (!"HOPDC".equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (!("DRAFT".equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
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
        if (!"HOPDC".equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // Kiểm tra xem Material có tồn tại không
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
        if ("DRAFT".equals(syllabus.getStatus())) {
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
        if ("STUDENT".equals(roleName) || "LECTURER".equals(roleName)) {
            if (!(SyllabusStatus.PUBLISHED.toString().equals(finalStatus))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(finalStatus)) {
            if (!"HOPDC".equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        List<Syllabus> syllabuses;

        // 3. Truy vấn dựa trên bộ lọc cuối cùng
        if (finalStatus != null) {
            // Tìm theo Subject ID và Status (AND)
            syllabuses = syllabusRepository.findBySubject_SubjectIdAndStatus(subjectId, finalStatus);
        } else {
            syllabuses = syllabusRepository.findBySubject_SubjectId(subjectId);
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
        if ("STUDENT".equals(account.getRole().getRoleName()) || "LECTURER".equals(account.getRole().getRoleName())) {
            if (!PloStatus.PUBLISHED.toString().equals(syllabus.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if ("DRAFT".equals(syllabus.getStatus())) {
            if (!"HOPDC".equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return syllabusRepository.findById(id)
                .map(syllabusMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
    }
}
