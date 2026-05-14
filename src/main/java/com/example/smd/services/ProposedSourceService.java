package com.example.smd.services;

import com.example.smd.dto.request.ProposedSourceRequest;
import com.example.smd.dto.response.SourceResponse;
import com.example.smd.entities.ProposedSource;
import com.example.smd.entities.Source;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SourceMapper;
import com.example.smd.repositories.ProposedSourceRepository;
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
public class ProposedSourceService {

    ProposedSourceRepository repository;
    SourceRepository sourceRepository;
    SubjectRepository subjectRepository;
    SourceMapper sourceMapper;

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------
    @Transactional
    public SourceResponse create(ProposedSourceRequest request) {
        Source source = sourceRepository.findById(request.getSourceId())
                .orElseThrow(() -> new AppException(ErrorCode.SOURCE_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (repository.existsBySource_SourceIdAndSubject_SubjectId(
                request.getSourceId(), request.getSubjectId())) {
            throw new AppException(ErrorCode.PROPOSED_SOURCE_ALREADY_EXISTS);
        }

        ProposedSource entity = ProposedSource.builder()
                .source(source)
                .subject(subject)
                .build();

        repository.save(entity);
        return sourceMapper.toResponse(source);
    }

    // -------------------------------------------------------
    // GET ALL (paginated + optional search)
    // -------------------------------------------------------
    public Page<SourceResponse> getAll(String search, int page, int size) {
        String searchCriteria = (search == null || search.trim().isEmpty()) ? null : search.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by("source.sourceName").ascending());
        // findByFilters dùng JOIN FETCH ps.source nên ps.getSource() an toàn
        return repository.findByFilters(searchCriteria, pageable)
                .map(ps -> sourceMapper.toResponse(ps.getSource()));
    }

    // -------------------------------------------------------
    // GET DETAIL
    // -------------------------------------------------------
    public SourceResponse getDetail(UUID id) {
        ProposedSource ps = repository.fetchWithSourceById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSED_SOURCE_NOT_FOUND));
        return sourceMapper.toResponse(ps.getSource());
    }

    // -------------------------------------------------------
    // GET BY SUBJECT
    // -------------------------------------------------------
    public List<SourceResponse> getBySubject(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        return repository.fetchWithSourceBySubjectId(subjectId)
                .stream()
                .map(ps -> sourceMapper.toResponse(ps.getSource()))
                .toList();
    }

    // -------------------------------------------------------
    // GET BY SOURCE
    // -------------------------------------------------------
    public List<SourceResponse> getBySource(UUID sourceId) {
        if (!sourceRepository.existsById(sourceId)) {
            throw new AppException(ErrorCode.SOURCE_NOT_FOUND);
        }
        return repository.fetchWithSourceBySourceId(sourceId)
                .stream()
                .map(ps -> sourceMapper.toResponse(ps.getSource()))
                .toList();
    }

    // -------------------------------------------------------
    // UPDATE (thay đổi source hoặc subject)
    // -------------------------------------------------------
    @Transactional
    public SourceResponse update(UUID id, ProposedSourceRequest request) {
        ProposedSource entity = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSED_SOURCE_NOT_FOUND));

        Source source = sourceRepository.findById(request.getSourceId())
                .orElseThrow(() -> new AppException(ErrorCode.SOURCE_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // Kiểm tra trùng lặp (ngoại trừ bản ghi hiện tại)
        boolean duplicateExists = repository
                .existsBySource_SourceIdAndSubject_SubjectId(request.getSourceId(), request.getSubjectId());
        boolean isSameRecord = entity.getSource().getSourceId().equals(request.getSourceId())
                && entity.getSubject().getSubjectId().equals(request.getSubjectId());

        if (duplicateExists && !isSameRecord) {
            throw new AppException(ErrorCode.PROPOSED_SOURCE_ALREADY_EXISTS);
        }

        entity.setSource(source);
        entity.setSubject(subject);
        repository.save(entity);
        return sourceMapper.toResponse(source);
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorCode.PROPOSED_SOURCE_NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
