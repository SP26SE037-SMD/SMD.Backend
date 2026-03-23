package com.example.smd.services;

import com.example.smd.dto.request.CloSessionMappingBatchRequest;
import com.example.smd.dto.request.CloSessionMappingRequest;
import com.example.smd.dto.response.clo.CloSessionMappingResponse;
import com.example.smd.entities.CLO_Session;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Session;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloSessionMappingMapper;
import com.example.smd.repositories.CLOsRepository;
import com.example.smd.repositories.CloSessionMappingRepository;
import com.example.smd.repositories.SessionRepository;
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
public class CloSessionMappingService {

    CloSessionMappingRepository repository;
    CloSessionMappingMapper mapper;
    CLOsRepository cloRepository;
    SessionRepository sessionRepository;
    SyllabusRepository syllabusRepository;

    @Transactional
    public CloSessionMappingResponse createMapping(CloSessionMappingRequest request) {
        UUID cloId = parseUuid(request.getCloId());
        UUID sessionId = parseUuid(request.getSessionId());

        if (repository.existsByClo_CloIdAndSession_SessionId(cloId, sessionId)) {
            throw new AppException(ErrorCode.MAPPING_ALREADY_EXISTS);
        }

        CLOs clo = cloRepository.findById(cloId)
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        CLO_Session entity = CLO_Session.builder()
                .clo(clo)
                .session(session)
                .build();

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public List<CloSessionMappingResponse> createBatch(CloSessionMappingBatchRequest request) {
        return request.getMappings().stream()
                .map(this::createMapping)
                .toList();
    }

    @Transactional
    public List<CloSessionMappingResponse> getBySyllabus(String syllabusId) {
        UUID id = parseUuid(syllabusId);
        if (!syllabusRepository.existsById(id)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        return repository.findBySession_Syllabus_SyllabusId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloSessionMappingResponse> getByClo(String cloId) {
        UUID id = parseUuid(cloId);
        if (!cloRepository.existsById(id)) {
            throw new AppException(ErrorCode.CLO_NOT_FOUND);
        }

        return repository.findByClo_CloId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloSessionMappingResponse> getBySession(String sessionId) {
        UUID id = parseUuid(sessionId);
        if (!sessionRepository.existsById(id)) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        }

        return repository.findBySession_SessionId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMapping(String id) {
        UUID mappingId = parseUuid(id);
        if (!repository.existsById(mappingId)) {
            throw new AppException(ErrorCode.MAPPING_NOT_FOUND);
        }
        repository.deleteById(mappingId);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
