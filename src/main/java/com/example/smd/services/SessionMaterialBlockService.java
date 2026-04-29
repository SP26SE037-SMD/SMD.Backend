package com.example.smd.services;

import com.example.smd.dto.request.session.SessionMaterialBlockBulkRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockBulkListRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockUpdateRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.dto.response.session.BulkSessionMaterialBlockResponse;
import com.example.smd.dto.response.session.SessionMaterialBlockDetailResponse;
import com.example.smd.entities.*;
import com.example.smd.enums.SessionType;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionMaterialBlockService {

    SessionRepository sessionRepository;
    SyllabusRepository syllabusRepository;
    MaterialRepository materialRepository;
    BlockRepository blockRepository;
    SubjectRepository subjectRepository;
    SessionMaterialBlockRepository sessionMaterialBlockRepository;
    SessionRegulationValidationService sessionRegulationValidationService;

    @Transactional
    public List<BulkSessionMaterialBlockResponse> bulkConfigureSessionMaterialBlocksByList(
            SessionMaterialBlockBulkListRequest request
    ) {
        List<BulkSessionMaterialBlockResponse> responses = new ArrayList<>();

        for (SessionMaterialBlockBulkListRequest.SessionItem item : request.getSessions()) {
            String newType = "";
            if (SessionType.THEORY.toString().equals(item.getSessionType())) {
                newType = SessionType.THEORY.toString();
            } else if (SessionType.PRACTICE.toString().equals(item.getSessionType())) {
                newType = SessionType.PRACTICE.toString();
            } else if (SessionType.SELF_STUDY.toString().equals(item.getSessionType())) {
                newType = SessionType.SELF_STUDY.toString();
            }

            SessionMaterialBlockBulkRequest singleRequest = SessionMaterialBlockBulkRequest.builder()
                    .syllabusId(request.getSyllabusId())
                    .sessionNumber(item.getSessionNumber())
                    .sessionTitle(item.getSessionTitle())
                    .teachingMethods(item.getTeachingMethods())
                    .duration(item.getDuration())
                    .sessionType(newType)
                    .material(item.getMaterial())
                    .block(item.getBlock())
                    .build();

            responses.add(bulkConfigureSessionMaterialBlocks(singleRequest));
        }

        return responses;
    }

    @Transactional
    public BulkSessionMaterialBlockResponse bulkConfigureSessionMaterialBlocks(SessionMaterialBlockBulkRequest request) {
        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (request.getMaterial() == null || request.getMaterial().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Material list is required");
        }
        if (request.getBlock() == null || request.getBlock().isEmpty()) {
            throw new AppException(ErrorCode.BLOCK_LIST_EMPTY);
        }

        Session session = sessionRepository
                .findBySyllabus_SyllabusIdAndSessionNumber(request.getSyllabusId(), request.getSessionNumber())
                .orElse(null);

        List<String> warnings = new ArrayList<>();
        if (session == null) {
            String newType = "";
            if (SessionType.THEORY.toString().equals(session.getSessionType())) {
                newType = SessionType.THEORY.toString();
            } else if (SessionType.PRACTICE.toString().equals(session.getSessionType())) {
                newType = SessionType.PRACTICE.toString();
            } else if (SessionType.SELF_STUDY.toString().equals(session.getSessionType())) {
                newType = SessionType.SELF_STUDY.toString();
            }

            session = Session.builder()
                    .syllabus(syllabus)
                    .sessionNumber(request.getSessionNumber())
                    .sessionTitle(request.getSessionTitle())
                    .teachingMethods(request.getTeachingMethods())
                    .duration(request.getDuration())
                    .sessionType(newType)
                    .status(SyllabusStatus.DRAFT.name())
                    .build();
            session = sessionRepository.save(session);
        } else {
            warnings.add("Session already exists. Reusing existing session by syllabusId + sessionNumber.");
        }

        List<Session_Material_Block> mappingsToSave = new ArrayList<>();
        List<BulkSessionMaterialBlockResponse.MappingError> errors = new ArrayList<>();

        for (UUID materialId : request.getMaterial()) {
            if (materialId == null) {
                errors.add(error("MATERIAL_ID_REQUIRED", "Material ID is required", null, null, null, null));
                continue;
            }

            Material material = materialRepository.findById(materialId).orElse(null);
            if (material == null) {
                errors.add(error("MATERIAL_NOT_FOUND", "Material not found", materialId, null, null, null));
                continue;
            }

            for (UUID blockId : request.getBlock()) {
                Blocks block = resolveBlock(material, blockId, errors);
                if (block == null) {
                    continue;
                }

                boolean exists = sessionMaterialBlockRepository
                        .existsBySession_SessionIdAndMaterial_MaterialIdAndBlock_BlockId(
                                session.getSessionId(), material.getMaterialId(), block.getBlockId());
                if (exists) {
                    warnings.add("Skipped duplicate mapping: materialId=" + material.getMaterialId() + ", blockId=" + block.getBlockId());
                    continue;
                }

                Session_Material_Block mapping = Session_Material_Block.builder()
                        .session(session)
                        .material(material)
                        .block(block)
                        .build();
                mappingsToSave.add(mapping);
            }
        }

        if (!mappingsToSave.isEmpty()) {
            sessionMaterialBlockRepository.saveAll(mappingsToSave);
        }

        return BulkSessionMaterialBlockResponse.builder()
                .success(errors.isEmpty())
                .sessionId(session.getSessionId())
                .totalMappingsCreated(mappingsToSave.size())
                .totalMaterialsProcessed(request.getMaterial().size())
                .errors(errors)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<SessionMaterialBlockDetailResponse> getBySyllabus(UUID syllabusId, int page, int size) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sessionNumber").ascending());
        Page<Session> sessions = sessionRepository.findBySyllabus_SyllabusId(syllabusId, pageable);

        List<UUID> sessionIds = sessions.getContent().stream().map(Session::getSessionId).toList();
        List<Session_Material_Block> mappings = sessionIds.isEmpty()
                ? List.of()
                : sessionMaterialBlockRepository.findAllBySession_SessionIdIn(sessionIds);

        Map<UUID, List<Session_Material_Block>> mappingBySessionId = new LinkedHashMap<>();
        for (Session_Material_Block mapping : mappings) {
            UUID sessionId = mapping.getSession().getSessionId();
            mappingBySessionId.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(mapping);
        }

        Page<SessionMaterialBlockDetailResponse> responsePage = sessions.map(session ->
                toDetailResponse(session, mappingBySessionId.getOrDefault(session.getSessionId(), List.of()))
        );

        return PagedResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public SessionMaterialBlockDetailResponse getSessionDetail(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        List<Session_Material_Block> mappings = sessionMaterialBlockRepository.findAllBySession_SessionId(sessionId);
        return toDetailResponse(session, mappings);
    }

    @Transactional
    public BulkSessionMaterialBlockResponse updateSessionMaterialBlocks(SessionMaterialBlockUpdateRequest request) {
        if (request.getMaterial() == null || request.getMaterial().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY, "Material list is required");
        }
        if (request.getBlock() == null || request.getBlock().isEmpty()) {
            throw new AppException(ErrorCode.BLOCK_LIST_EMPTY);
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        UUID syllabusId = session.getSyllabus().getSyllabusId();
        boolean duplicatedSessionNumber = sessionRepository
                .existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
                        syllabusId,
                        request.getSessionNumber(),
                        session.getSessionId()
                );
        if (duplicatedSessionNumber) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

//        sessionRegulationValidationService.validateDurationByRegulation(
//            syllabusId,
//            request.getDuration(),
//            session.getSessionId()
//        );

        String newType = "";
        if (SessionType.THEORY.toString().equals(request.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        session.setSessionNumber(request.getSessionNumber());
        session.setSessionTitle(request.getSessionTitle());
        session.setTeachingMethods(request.getTeachingMethods());
        session.setDuration(request.getDuration());
        session.setSessionType(newType);
        sessionRepository.save(session);

        sessionMaterialBlockRepository.deleteBySession_SessionId(session.getSessionId());

        List<String> warnings = new ArrayList<>();
        List<BulkSessionMaterialBlockResponse.MappingError> errors = new ArrayList<>();
        List<Session_Material_Block> mappingsToSave = new ArrayList<>();
        Set<String> requestPairKeys = new HashSet<>();

        for (UUID materialId : request.getMaterial()) {
            if (materialId == null) {
                errors.add(error("MATERIAL_ID_REQUIRED", "Material ID is required", null, null, null, null));
                continue;
            }

            Material material = materialRepository.findById(materialId).orElse(null);
            if (material == null) {
                errors.add(error("MATERIAL_NOT_FOUND", "Material not found", materialId, null, null, null));
                continue;
            }

            for (UUID blockId : request.getBlock()) {
                Blocks block = resolveBlock(material, blockId, errors);
                if (block == null) {
                    continue;
                }

                String pairKey = material.getMaterialId() + "::" + block.getBlockId();
                if (!requestPairKeys.add(pairKey)) {
                    warnings.add("Skipped duplicate mapping in request: materialId="
                            + material.getMaterialId() + ", blockId=" + block.getBlockId());
                    continue;
                }

                mappingsToSave.add(Session_Material_Block.builder()
                        .session(session)
                        .material(material)
                        .block(block)
                        .build());
            }
        }

        if (!mappingsToSave.isEmpty()) {
            sessionMaterialBlockRepository.saveAll(mappingsToSave);
        }

        return BulkSessionMaterialBlockResponse.builder()
                .success(errors.isEmpty())
                .sessionId(session.getSessionId())
                .totalMappingsCreated(mappingsToSave.size())
                .totalMaterialsProcessed(request.getMaterial().size())
                .errors(errors)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    private SessionMaterialBlockDetailResponse toDetailResponse(Session session, List<Session_Material_Block> mappings) {
        Map<UUID, SessionMaterialBlockDetailResponse.MaterialItem> materialMap = new LinkedHashMap<>();
        Map<UUID, SessionMaterialBlockDetailResponse.BlockItem> blockMap = new LinkedHashMap<>();

        String newType = "";
        if (SessionType.THEORY.toString().equals(session.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(session.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(session.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        for (Session_Material_Block mapping : mappings) {
            Material material = mapping.getMaterial();
            if (material != null && material.getMaterialId() != null) {
                materialMap.putIfAbsent(
                        material.getMaterialId(),
                        SessionMaterialBlockDetailResponse.MaterialItem.builder()
                                .materialId(material.getMaterialId())
                                .materialName(material.getTitle())
                                .build()
                );
            }

            Blocks block = mapping.getBlock();
            if (block != null && block.getBlockId() != null) {
                blockMap.putIfAbsent(
                        block.getBlockId(),
                        SessionMaterialBlockDetailResponse.BlockItem.builder()
                                .blockId(block.getBlockId())
                                .content(block.getContentText())
                                .idx(block.getIdx())
                                .build()
                );
            }
        }

        return SessionMaterialBlockDetailResponse.builder()
                .session(session.getSessionId())
                .sessionNumber(session.getSessionNumber())
                .sessionTitle(session.getSessionTitle())
                .teachingMethods(session.getTeachingMethods())
                .sessionType(newType)
                .duration(session.getDuration())
                .material(new ArrayList<>(materialMap.values()))
                .block(new ArrayList<>(blockMap.values()))
                .status(session.getStatus())
                .build();
    }

    private Blocks resolveBlock(
            Material material,
            UUID blockItem,
            List<BulkSessionMaterialBlockResponse.MappingError> errors
    ) {
        if (blockItem == null) {
            errors.add(error("BLOCK_REQUIRED", "Block item is required", material.getMaterialId(), null, null, null));
            return null;
        }

            Blocks existingBlock = blockRepository.findById(blockItem).orElse(null);
            if (existingBlock == null) {
                errors.add(error("BLOCK_NOT_FOUND", "Block not found", material.getMaterialId(), blockItem , null, null));
                return null;
            }
            if (existingBlock.getMaterial() == null || !material.getMaterialId().equals(existingBlock.getMaterial().getMaterialId())) {
                errors.add(error("INVALID_BLOCK_MATERIAL", "Block " +
                        "does not belong to provided material",
                        material.getMaterialId(), blockItem,null,
                        null));
                return null;
            }
            return existingBlock;

    }

    private BulkSessionMaterialBlockResponse.MappingError error(
            String errorCode,
            String errorMessage,
            UUID materialId,
            UUID blockId,
            Integer idx,
            String details
    ) {
        return BulkSessionMaterialBlockResponse.MappingError.builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .materialId(materialId)
                .blockId(blockId)
                .idx(idx)
                .details(details)
                .build();
    }

    public SessionValidationResult validate(List<SessionMaterialBlockBulkRequest> inputs, UUID syllabusId) {
        SessionValidationResult result = new SessionValidationResult();

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        Subject masterSubject = subjectRepository.findById(syllabus.getSubject().getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 1. Tính quỹ Lý thuyết (Quy đổi an toàn từ Giờ -> Tiết)
        double totalTheoryHours = inputs.stream()
                .filter(s -> "THEORY".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0) // Dùng Double để nhận số lẻ 1.5, 2.25
                .sum();

        int totalTheoryPeriods = (int) Math.round(totalTheoryHours / 45);
        int remainingTheory = masterSubject.getTheoryPeriods() - totalTheoryPeriods;

        // 2. Tính quỹ Thực hành (Tương tự)
        double totalPracticeHours = inputs.stream()
                .filter(s -> "PRACTICE".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();

        int totalPracticePeriods = (int) Math.round(totalPracticeHours / 45);
        int remainingPractice = masterSubject.getPracticalPeriods() - totalPracticePeriods;

        // (Tùy chọn) Tính tổng giờ tự học nếu có bắt validate
        int totalSelfStudyHours = inputs.stream()
                .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
                .sum();

        int remainingSelfStudy = masterSubject.getSelfStudyPeriods() - totalSelfStudyHours;
        // Set vào DTO
        result.setRemainingQuotas(new SessionValidationResult.RemainingQuota(remainingTheory, remainingPractice, 0));

        // 2. Viết Logic Check Lỗi

        // -- Validate Lý thuyết (Theory) --
        if (remainingTheory > 0) {
            // Trường hợp THIẾU (Allocated < Quota)
            result.addError("THEORY_SHORTAGE",
                    "Theory allocation is short by " + remainingTheory + " period(s).");
        } else if (remainingTheory < 0) {
            // Trường hợp DƯ (Allocated > Quota)
            result.addError("THEORY_SURPLUS",
                    "Theory allocation exceeded by " + Math.abs(remainingTheory) + " period(s).");
        }

        // -- Validate Thực hành (Practice) --
        if (remainingPractice > 0) {
            // Trường hợp THIẾU
            result.addError("PRACTICE_SHORTAGE",
                    "Practice allocation is short by " + remainingPractice + " period(s).");
        } else if (remainingPractice < 0) {
            // Trường hợp DƯ
            result.addError("PRACTICE_SURPLUS",
                    "Practice allocation exceeded by " + Math.abs(remainingPractice) + " period(s).");
        }

        // -- Validate Tự học (Self-study) --
        if (remainingSelfStudy > 0) {
            // Trường hợp THIẾU
            result.addError("SELF_STUDY_SHORTAGE",
                    "Self-study allocation is short by " + remainingSelfStudy + " hour(s).");
        } else if (remainingSelfStudy < 0) {
            // Trường hợp DƯ
            result.addError("SELF_STUDY_SURPLUS",
                    "Self-study allocation exceeded by " + Math.abs(remainingSelfStudy) + " hour(s).");
        }

        return result;
    }
}
