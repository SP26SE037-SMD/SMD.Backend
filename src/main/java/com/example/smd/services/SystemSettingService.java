package com.example.smd.services;

import com.example.smd.dto.request.SystemSettingRequest;
import com.example.smd.dto.response.SystemSettingResponse;
import com.example.smd.entities.System_Setting;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SystemSettingMapper;
import com.example.smd.repositories.SystemSettingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemSettingService {
    SystemSettingRepository systemSettingRepository;
    SystemSettingMapper systemSettingMapper;

    @Transactional(readOnly = true)
    public Page<SystemSettingResponse> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<System_Setting> entityPage;
        if (search == null || search.trim().isEmpty()) {
            entityPage = systemSettingRepository.findAll(pageable);
        } else {
            entityPage = systemSettingRepository.findAllByCodeContainingIgnoreCaseOrValueContainingIgnoreCase(search, search, pageable);
        }
        return entityPage.map(systemSettingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse getDetail(UUID id) {
        System_Setting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Setting not found"));
        return systemSettingMapper.toResponse(setting);
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse getDetailByCode(String code) {
        System_Setting setting = systemSettingRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Setting not found"));
        return systemSettingMapper.toResponse(setting);
    }

    @Transactional
    public SystemSettingResponse update(UUID id, SystemSettingRequest request) {
        System_Setting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Setting not found"));
        
        systemSettingMapper.updateEntity(setting, request);
        return systemSettingMapper.toResponse(systemSettingRepository.save(setting));
    }
}
