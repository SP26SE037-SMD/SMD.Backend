package com.example.smd.services;

import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
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
//    SyllabusActionLog syllabusActionLog;

    // 1. Tạo mới
    @Transactional
    public SyllabusResponse create(SyllabusRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        Syllabus syllabus = syllabusMapper.toSyllabus(request);
        syllabus.setSubject(subject);
        syllabus.setStatus("DRAFT");

        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    // 2. Cập nhật thông tin chung
    @Transactional
    public SyllabusResponse update(UUID id, SyllabusRequest request) {
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

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
        return  syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    // 4. Xóa đệm (Soft Delete)
    @Transactional
    public void delete(UUID id) {
        try {
            // Kiểm tra xem Material có tồn tại không
            Syllabus syllabus = syllabusRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
            if(syllabus.getStatus().equals("DRAFT")) {
                syllabusRepository.delete(syllabus);
            } else{
                syllabus.setStatus("ARCHIVED");
                syllabusRepository.save(syllabus);
            }

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // 5. Get All by Subject
    @Transactional
    public List<SyllabusResponse> getAllBySubject(UUID subjectId) {
        // 1. Kiểm tra xem môn học có tồn tại trong hệ thống không
        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        // 2. Nếu tồn tại, mới tiến hành lấy danh sách Syllabus
        return syllabusRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .map(syllabusMapper::toResponse)
                .toList();
    }

    // 6. Get Detail
    @Transactional
    public SyllabusResponse getDetail(UUID id) {
        return syllabusRepository.findById(id)
                .map(syllabusMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
    }
}
