package com.example.smd.services;

import com.example.smd.dto.request.CloSessionMappingBatchRequest;
import com.example.smd.dto.request.CloSessionMappingRequest;
import com.example.smd.dto.response.clo.CloSessionMappingResponse;
import com.example.smd.dto.response.validate.SessionCloMappingValidationResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloSessionMappingMapper;
import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloSessionMappingService {

    CloSessionMappingRepository repository;
    CloSessionMappingMapper mapper;
    CLOsRepository cloRepository;
    SessionRepository sessionRepository;
    SyllabusRepository syllabusRepository;
    CloSessionMappingExecutor cloSessionMapping;

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

    public String startCLOSessionMappingProcess(List<CloSessionMappingRequest> request, UUID syllabusId, String accountId) throws IOException {
        cloSessionMapping.checkMapping(request, syllabusId, accountId);
        return "The system is processing the CLO-Session-Mapping, please wait for a notification!";
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class CloSessionMappingExecutor {

    private final SyllabusRepository syllabusRepository;
    private final SubjectRepository subjectRepository;
    private final CLOsRepository cloRepository;
    private final SessionRepository sessionRepository;
    private final GeminiService geminiService;
    private final RealtimePublisher realtimePublisher;

    @Async
    @Transactional
    public void checkMapping(List<CloSessionMappingRequest> request, UUID syllabusId, String accountId) {

        realtimePublisher.publishToAccount(accountId,
                RealtimePayload.status("VALIDATE_MAPPING_PROCESS", "Currently being processed."));
        log.info("VALIDATE_MAPPING_PROCESS: {}", "Currently being processed.");

        // Lấy dữ liệu từ DB (Chạy loáng một cái là xong, DB Connection giải phóng liền)
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
        Subject subject = subjectRepository.findById(syllabus.getSubject().getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        List<CLOs> cloList = cloRepository.findBySubject_SubjectId(subject.getSubjectId());
        List<Map<String, String>> cloJsonData = cloList.stream().map(clo -> {
            Map<String, String> map = new HashMap<>();
            map.put("clo_id", clo.getCloId().toString());
            map.put("clo_code", clo.getCloCode());
            map.put("description", clo.getDescription());
            map.put("bloom_level", clo.getBloomLevel());
            return map;
        }).collect(Collectors.toList());

        List<Session> sessionList = sessionRepository.findBySyllabus_SyllabusId(syllabusId);
        List<Map<String, String>> sessionJsonData = sessionList.stream().map(a -> {
            Map<String, String> map = new HashMap<>();
            map.put("session_id", a.getSessionId().toString());
            map.put("chapter_title", a.getSessionTitle());
            map.put("session_topic", a.getSessionTopic());
            map.put("teaching_method", a.getTeachingMethods());
            return map;
        }).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String currentMapping = buildSessionCloMappingForAI(request);
            String sessionJsonString = objectMapper.writeValueAsString(sessionJsonData);
            String cloJsonString = objectMapper.writeValueAsString(cloJsonData);

            var sessionMappingResult = geminiService.checkSessionCloMapping(sessionJsonString, cloJsonString, currentMapping, accountId);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    realtimePublisher.publishToAccount(accountId,
                            RealtimePayload.status("VALIDATE_MAPPING_SUCCESS", sessionMappingResult));
                    log.info("VALIDATE_MAPPING_SUCCESS: {}", sessionMappingResult);
                }
            });


        } catch (JsonProcessingException e) {
            realtimePublisher.publishToAccount(accountId,
                    RealtimePayload.status("VALIDATE_MAPPING_FAIL", "Failed to parse JSON data, please try again!"));
            log.error("Lỗi khi parse đối tượng sang JSON String", e);
        }
    }

    // Hàm bổ trợ này đi theo hàm checkMapping sang class mới luôn
    public String buildSessionCloMappingForAI(List<CloSessionMappingRequest> requests) {
        Set<UUID> sessionIds = requests.stream()
                .map(req -> UUID.fromString(req.getSessionId()))
                .collect(Collectors.toSet());

        Set<UUID> cloIds = requests.stream()
                .map(req -> UUID.fromString(req.getCloId()))
                .collect(Collectors.toSet());

        List<Session> sessionFromDb = sessionRepository.findAllById(sessionIds);
        List<CLOs> closFromDb = cloRepository.findAllById(cloIds);

        Map<String, String> sessionIdMap = sessionFromDb.stream()
                .collect(Collectors.toMap(a -> a.getSessionId().toString(), a -> a.getSessionId().toString()));

        Map<String, String> cloIdToCodeMap = closFromDb.stream()
                .collect(Collectors.toMap(clo -> clo.getCloId().toString(), CLOs::getCloCode));

        Map<String, List<String>> mappingResult = requests.stream()
                .filter(req -> sessionIdMap.containsKey(req.getSessionId().toString())
                        && cloIdToCodeMap.containsKey(req.getCloId().toString()))
                .collect(Collectors.groupingBy(
                        req -> req.getSessionId().toString(),
                        Collectors.mapping(req -> cloIdToCodeMap.get(req.getCloId().toString()), Collectors.toList())
                ));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mappingResult);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse mapping data cho AI Prompt", e);
        }
    }
}
