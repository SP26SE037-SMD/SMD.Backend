package com.example.smd.services;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.response.syllabus.SyllabusActionLogResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Syllabus_Action_Logs;
import com.example.smd.enums.SyllabusActionType;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SyllabusActionLogMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.SyllabusActionLogRepository;
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
public class SyllabusActionLogService {
    SyllabusActionLogRepository logRepository;
    SyllabusRepository syllabusRepository;
    AccountRepository accountRepository; // Giả định bạn có AccountRepo
    SyllabusActionLogMapper logMapper;

    @Transactional
    public SyllabusActionLogResponse createLog(SyllabusActionLogRequest request) {
        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        // Giả định lấy User hiện tại từ Context hoặc Repo
        Account account = accountRepository.findByEmail(request.getActionByEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Syllabus_Action_Logs log = logMapper.toEntity(request);
        log.setSyllabus(syllabus);
        log.setActionBy(account);

        SyllabusActionType type;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            type = SyllabusActionType.valueOf(request.getActionType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_STATUS_INPUT);
        }
        log.setAction(type.toString());
        log.setCreatedAt(java.time.Instant.now());

        return logMapper.toResponse(logRepository.save(log));
    }

    public List<SyllabusActionLogResponse> getLogsBySyllabus(UUID syllabusId) {
        return logRepository.findBySyllabus_SyllabusIdOrderByCreatedAtDesc(syllabusId)
                .stream()
                .map(log -> {
                    var res = logMapper.toResponse(log);
                    res.setNote(SyllabusActionType.valueOf(log.getAction()).getDescription());
                    return res;
                }).toList();
    }

    public SyllabusActionLogResponse getDetail(UUID id) {
        return logRepository.findById(id)
                .map(log -> {
                    var res = logMapper.toResponse(log);
                    res.setNote(SyllabusActionType.valueOf(log.getAction()).getDescription());
                    return res;
                })
                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND));
    }

    @Transactional
    public SyllabusActionLogResponse updateNote(UUID id, String note) {
        Syllabus_Action_Logs log = logRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND));
        log.setNote(note);
        return logMapper.toResponse(logRepository.save(log));
    }

    @Transactional
    public void deleteLog(UUID id) {
        logRepository.deleteById(id);
    }

    public SyllabusActionType mapStatusToAction(String status) {

        SyllabusStatus syllabusStatus = SyllabusStatus.valueOf(status.toUpperCase());

        return switch (syllabusStatus) {
            case PENDING_REVIEW -> SyllabusActionType.SUBMIT;
            case REVISION_REQUESTED -> SyllabusActionType.REQUEST_REVISION;
            case APPROVED -> SyllabusActionType.APPROVE;
            case REJECTED -> SyllabusActionType.REJECT;
            case PUBLISHED -> SyllabusActionType.PUBLISH;
            default -> SyllabusActionType.CREATE; // Mặc định cho DRAFT hoặc tạo mới
        };
    }
}
