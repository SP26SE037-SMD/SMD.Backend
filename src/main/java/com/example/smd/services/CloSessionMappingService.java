package com.example.smd.services;

import com.example.smd.dto.request.CloSessionMappingBatchRequest;
import com.example.smd.dto.request.CloSessionMappingRequest;
import com.example.smd.dto.response.clo.CloSessionMappingResponse;
import com.example.smd.dto.response.validate.SessionCloMappingValidationResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloSessionMappingMapper;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloSessionMappingService {

    CloSessionMappingRepository repository;
    CloSessionMappingMapper mapper;
    CLOsRepository cloRepository;
    SessionRepository sessionRepository;
    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    GeminiService geminiService;

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

    public SessionCloMappingValidationResult checkMapping (List<CloSessionMappingRequest> request, UUID syllabusId) {

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
            return map;
        }).collect(Collectors.toList());

        List<Session> sessionList = sessionRepository.findBySyllabus_SyllabusId(syllabusId);
        List<Map<String, String>> sessionJsonData = sessionList.stream().map(a -> {
            Map<String, String> map = new HashMap<>();
            map.put("session_id", a.getSessionId().toString());
            map.put("chapter_title", a.getSessionTitle()); // Type của câu hỏi
            map.put("session_topic", a.getSessionTopic()); // Kỹ năng cốt lõi
            map.put("teaching_method", a.getTeachingMethods()); // Rubric/Hướng dẫn chấm
            return map;
        }).collect(Collectors.toList());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String currentMapping = buildSessionCloMappingForAI(request);
            String sessionJsonString = objectMapper.writeValueAsString(sessionJsonData);
            String cloJsonString = objectMapper.writeValueAsString(cloJsonData);

            return geminiService.checkSessionCloMapping(sessionJsonString, cloJsonString, currentMapping);
        } catch (JsonProcessingException e) {
            // Bắn ra lỗi Runtime hoặc Custom Exception của hệ thống bác
            throw new RuntimeException("Lỗi khi parse đối tượng sang JSON String", e);
        }
    }

    public String buildSessionCloMappingForAI(List<CloSessionMappingRequest> requests) {

        // 1. LẤY ID TỪ REQUEST
        Set<UUID> sessionIds = requests.stream()
                .map(req -> UUID.fromString(req.getSessionId()))
                .collect(Collectors.toSet());

        Set<UUID> cloIds = requests.stream()
                .map(req -> UUID.fromString(req.getCloId()))
                .collect(Collectors.toSet());

        // 2. GỌI DATABASE ĐỂ LẤY THỰC THỂ (Optional: Dùng hàm JOIN FETCH để tránh LazyLoad)
        List<Session> sessionFromDb = sessionRepository.findAllById(sessionIds);
        List<CLOs> closFromDb = cloRepository.findAllById(cloIds);

        // 3. TẠO MAP CHUYỂN ĐỔI: Chuyển ID (UUID) sang ID dạng String để đối chiếu
        Map<String, String> sessionIdMap = sessionFromDb.stream()
                .collect(Collectors.toMap(a -> a.getSessionId().toString(), a -> a.getSessionId().toString()));

        Map<String, String> cloIdToCodeMap = closFromDb.stream()
                .collect(Collectors.toMap(clo -> clo.getCloId().toString(), CLOs::getCloCode));

        // 4. GOM NHÓM DỮ LIỆU: KEY = Session ID, VALUE = List<CLO Code>
        Map<String, List<String>> mappingResult = requests.stream()
                .filter(req -> sessionIdMap.containsKey(req.getSessionId().toString())
                        && cloIdToCodeMap.containsKey(req.getCloId().toString()))
                .collect(Collectors.groupingBy(
                        req -> req.getSessionId().toString(),
                        Collectors.mapping(req -> cloIdToCodeMap.get(req.getCloId().toString()), Collectors.toList())
                ));

        // 5. PARSE SANG JSON STRING
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mappingResult);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse mapping data cho AI Prompt", e);
        }
    }
}
