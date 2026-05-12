package com.example.smd.services;

import com.example.smd.dto.request.request.RequestCreateRequest;
import com.example.smd.dto.request.request.RequestUpdateRequest;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.entities.Request;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.RequestMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.RequestRepository;
import com.example.smd.repositories.RequestSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestService {

    RequestRepository requestRepository;
    AccountRepository accountRepository;
    RequestMapper requestMapper;

    // ------------------------------------------------------------------ CREATE

    @Transactional
    public RequestResponse create(RequestCreateRequest dto, String createdByUserId) {
        Request request = requestMapper.toEntity(dto);

        // createdBy từ JWT
        request.setCreatedBy(
                accountRepository.findById(UUID.fromString(createdByUserId))
                        .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));

        // receivedBy (tuỳ chọn)
        if (dto.getReceivedById() != null) {
            request.setReceivedBy(
                    accountRepository.findById(dto.getReceivedById())
                            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        request.setStatus("PENDING");

        return requestMapper.toResponse(requestRepository.save(request));
    }

    // ------------------------------------------------------------------ READ

    @Transactional(readOnly = true)
    public Page<RequestResponse> getAll(
            String search,
            String status,
            String type,
            UUID createdById,
            UUID receivedById,
            UUID targetId,
            Pageable pageable) {

        var spec = RequestSpecification.withFilters(search, status, type, createdById, receivedById, targetId);
        return requestRepository.findAll(spec, pageable)
                .map(requestMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(UUID id) {
        return requestMapper.toResponse(
                requestRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND)));
    }

    // ------------------------------------------------------------------ UPDATE STATUS

    /**
     * Chỉ người nhận (receivedBy) mới được phép cập nhật trạng thái + comment.
     * Các trường khác (title, content, type, targetId) không được thay đổi ở đây.
     */
    @Transactional
    public RequestResponse updateStatus(UUID id, RequestUpdateRequest dto) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        requestMapper.updateEntity(request, dto);

        if (dto.getReceivedById() != null) {
            request.setReceivedBy(
                    accountRepository.findById(dto.getReceivedById())
                            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        return requestMapper.toResponse(requestRepository.save(request));
    }

    // ------------------------------------------------------------------ DELETE

    @Transactional
    public void delete(UUID id) {
        if (!requestRepository.existsById(id)) {
            throw new AppException(ErrorCode.REQUEST_NOT_FOUND);
        }
        requestRepository.deleteById(id);
    }
}
