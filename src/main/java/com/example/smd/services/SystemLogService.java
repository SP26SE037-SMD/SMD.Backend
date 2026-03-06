package com.example.smd.services;

import com.example.smd.dto.request.SystemLogRequest;
import com.example.smd.dto.response.SystemLogResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.System_Log;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SystemLogMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.SystemLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemLogService {

    SystemLogRepository systemLogRepository;
    AccountRepository accountRepository;
    SystemLogMapper systemLogMapper;

    /**
     * Ghi log hoạt động của user
     */
    @Transactional
    public SystemLogResponse createLog(SystemLogRequest request) {
        Account account;

        // Nếu không chỉ định accountId, lấy từ user đang đăng nhập
        if (request.getAccountId() == null) {
            account = getCurrentAccount();
        } else {
            account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        }

        System_Log systemLog = systemLogMapper.toSystemLog(request);
        systemLog.setAccount(account);

        System_Log savedLog = systemLogRepository.save(systemLog);
        log.info("Logged action: {} by user: {}", savedLog.getAction(), account.getEmail());

        return systemLogMapper.toSystemLogResponse(savedLog);
    }

    /**
     * Lấy tất cả log với phân trang
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<System_Log> logs = systemLogRepository.findAll(pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy log của một user cụ thể
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getLogsByAccount(UUID accountId, int page, int size) {
        // Kiểm tra account có tồn tại không
        if (!accountRepository.existsById(accountId)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.findByAccountAccountIdOrderByCreatedAtDesc(accountId, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy log theo targetId (tất cả hoạt động liên quan đến một đối tượng)
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getLogsByTargetId(UUID targetId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy log của user hiện tại
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getMyLogs(int page, int size) {
        UUID currentUserId = getCurrentAccount().getAccountId();
        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.findByAccountAccountIdOrderByCreatedAtDesc(currentUserId, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Tìm kiếm log theo action hoặc objectName
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> searchLogs(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.searchLogs(search, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy log trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getLogsByDateRange(Instant startDate, Instant endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startDate, endDate, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy log của user trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public Page<SystemLogResponse> getLogsByAccountAndDateRange(
            UUID accountId, Instant startDate, Instant endDate, int page, int size) {
        // Kiểm tra account có tồn tại không
        if (!accountRepository.existsById(accountId)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<System_Log> logs = systemLogRepository.findByAccountAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                accountId, startDate, endDate, pageable);
        return logs.map(systemLogMapper::toSystemLogResponse);
    }

    /**
     * Lấy chi tiết một log
     */
    @Transactional(readOnly = true)
    public SystemLogResponse getLogDetail(UUID logId) {
        System_Log systemLog = systemLogRepository.findById(logId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND));
        return systemLogMapper.toSystemLogResponse(systemLog);
    }

    /**
     * Lấy thông tin account hiện tại từ SecurityContext
     */
    private Account getCurrentAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
