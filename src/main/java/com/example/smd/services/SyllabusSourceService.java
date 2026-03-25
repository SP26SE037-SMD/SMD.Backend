package com.example.smd.services;

import com.example.smd.dto.response.SourceResponse;
import com.example.smd.entities.Source;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Syllabus_Source;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SourceMapper;
import com.example.smd.repositories.SourceRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.repositories.SyllabusSourceRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SyllabusSourceService {
    SyllabusSourceRepository syllabusSourceRepository;
    SyllabusRepository syllabusRepository;
    SourceRepository sourceRepository;
    SourceMapper sourceMapper;
    AccountService accountService;

    // 1. Thêm danh sách Source vào Syllabus
    @Transactional
    public void addSourcesToSyllabus(UUID syllabusId, List<UUID> sourceIds, String accountId) {
        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!roleName.equals("HOPDC")) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if(!(syllabus.getStatus().equals("DRAFT") || syllabus.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_EDITABLE);
        }

        for (UUID sourceId : sourceIds) {
            Source source = sourceRepository.findById(sourceId)
                    .orElseThrow(() -> new AppException(ErrorCode.SOURCE_NOT_FOUND));

            if (syllabusSourceRepository.existsBySyllabus_SyllabusIdAndSource_SourceId(syllabusId, sourceId)) {
                log.warn("Source {} already exists in syllabus {}", sourceId, syllabusId);
                throw new AppException(ErrorCode.SOURCE_ALREADY_MAPPED);
            }

            Syllabus_Source mapping = Syllabus_Source.builder()
                    .syllabus(syllabus)
                    .source(source)
                    .build();
            syllabusSourceRepository.save(mapping);
        }
    }

    // 2. Xóa Source khỏi Syllabus
    @Transactional
    public void removeSourceFromSyllabus(UUID syllabusId, UUID sourceId, String accountId) {

        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!roleName.equals("HOPDC")) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if(!(syllabus.getStatus().equals("DRAFT") || syllabus.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_EDITABLE);
        }

        Syllabus_Source mapping = syllabusSourceRepository.findBySyllabus_SyllabusIdAndSource_SourceId(syllabusId, sourceId)
                .orElseThrow(() -> new AppException(ErrorCode.MAPPING_NOT_FOUND));

        syllabusSourceRepository.delete(mapping);
    }

    // 3. Xem danh sách Source bằng SyllabusId
    @Transactional
    public List<SourceResponse> getSourcesBySyllabusId(UUID syllabusId) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        return syllabusSourceRepository.findBySyllabus_SyllabusId(syllabusId).stream()
                .map(mapping -> sourceMapper.toResponse(mapping.getSource()))
                .toList();
    }
}
