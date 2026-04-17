package com.example.smd.services;

import com.example.smd.dto.request.request.RequestRequest;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.entities.Request;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.RequestMapper;
import com.example.smd.repositories.*;
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
    CurriculumRepository curriculumRepository;
    MajorRepository majorRepository;
    RequestMapper requestMapper;

    @Transactional
    public RequestResponse create(RequestRequest requestDto) {
        Request request = requestMapper.toRequest(requestDto);

        if (requestDto.getCreatedById() != null) {
            request.setCreatedBy(accountRepository.findById(requestDto.getCreatedById())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        if (requestDto.getCurriculumId() != null) {
            request.setCurriculum(curriculumRepository.findById(requestDto.getCurriculumId())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND)));
        }

        if (requestDto.getMajorId() != null) {
            request.setMajor(majorRepository.findById(requestDto.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND)));
        }

        request = requestRepository.save(request);
        return requestMapper.toRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponse> getAll(String search, String status, UUID curriculumId, UUID majorId, Pageable pageable) {
        var spec = RequestSpecification.withFilters(search, status, curriculumId, majorId);
        return requestRepository.findAll(spec, pageable).map(requestMapper::toRequestResponse);
    }

    @Transactional(readOnly = true)
    public RequestResponse getDetail(UUID id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));
        return requestMapper.toRequestResponse(request);
    }

    @Transactional
    public RequestResponse update(UUID id, RequestRequest requestDto) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        requestMapper.updateRequest(request, requestDto);

        if (requestDto.getCreatedById() != null) {
            request.setCreatedBy(accountRepository.findById(requestDto.getCreatedById())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        if (requestDto.getCurriculumId() != null) {
            request.setCurriculum(curriculumRepository.findById(requestDto.getCurriculumId())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND)));
        }

        if (requestDto.getMajorId() != null) {
            request.setMajor(majorRepository.findById(requestDto.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND)));
        }

        request = requestRepository.save(request);
        return requestMapper.toRequestResponse(request);
    }

    @Transactional
    public void delete(UUID id) {
        if (!requestRepository.existsById(id)) {
            throw new AppException(ErrorCode.REQUEST_NOT_FOUND);
        }
        requestRepository.deleteById(id);
    }

    @Transactional
    public RequestResponse updateStatus(UUID id, String status) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));
        request.setStatus(status);
        request = requestRepository.save(request);
        return requestMapper.toRequestResponse(request);
    }
}
