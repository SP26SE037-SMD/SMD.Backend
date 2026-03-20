package com.example.smd.services;

import com.example.smd.dto.request.SourceRequest;
import com.example.smd.dto.response.SourceResponse;
import com.example.smd.entities.Source;
import com.example.smd.enums.SourceType;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SourceMapper;
import com.example.smd.repositories.SourceRepository;
import com.example.smd.repositories.SubjectRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SourceService {
    SourceRepository repository;
    SubjectRepository subjectRepository;
    SourceMapper mapper;

    @Transactional
    public SourceResponse create(SourceRequest request) {
        SourceType type;
        try {
            type = SourceType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_SOURCE_TYPE);
        }
        Source source = mapper.toSource(request);
        source.setType(type.toString());
        return mapper.toResponse(repository.save(source));
    }

    public Page<SourceResponse> getAll(String search, String type, int page, int size) {
        String typeStr = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                typeStr = SourceType.valueOf(type.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_SOURCE_TYPE);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sourceName").ascending());

        // Nếu search rỗng thì để null để Repo bỏ qua điều kiện
        String searchCriteria = (search == null || search.trim().isEmpty()) ? null : search.trim();

        return repository.findByFilters(typeStr, searchCriteria, pageable)
                .map(mapper::toResponse);
    }

    public SourceResponse getDetail(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SOURCE_NOT_FOUND));
    }

    @Transactional
    public SourceResponse update(UUID id, SourceRequest request) {
        Source source = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SOURCE_NOT_FOUND));

        SourceType type;
        try {
            type = SourceType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_SOURCE_TYPE);
        }
        request.setType(type.toString());
        mapper.updateSource(source, request);
        return mapper.toResponse(repository.save(source));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorCode.SOURCE_NOT_FOUND);
        }
        repository.deleteById(id);
    }

    public List<SourceResponse> getSourcesBySubject(UUID subjectId) {

        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        List<Source> sources = repository.findAllBySubjectId(subjectId);

        return sources.stream()
                .map(mapper::toResponse)
                .toList();
    }
}
