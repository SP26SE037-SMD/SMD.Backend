package com.example.smd.services;

import com.example.smd.dto.request.session.SessionMaterialBlockBulkRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockUpdateRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.session.BulkSessionMaterialBlockResponse;
import com.example.smd.dto.response.session.SessionMaterialBlockDetailResponse;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Material;
import com.example.smd.entities.Session;
import com.example.smd.entities.Session_Material_Block;
import com.example.smd.entities.Syllabus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.MaterialRepository;
import com.example.smd.repositories.SessionMaterialBlockRepository;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SyllabusRepository;
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
    SessionMaterialBlockRepository sessionMaterialBlockRepository;
    SessionRegulationValidationService sessionRegulationValidationService;

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
            sessionRegulationValidationService.validateDurationByRegulation(
                request.getSyllabusId(),
                request.getDuration(),
                null
            );

            session = Session.builder()
                    .syllabus(syllabus)
                    .sessionNumber(request.getSessionNumber())
                    .sessionTitle(request.getSessionTitle())
                    .teachingMethods(request.getTeachingMethods())
                    .duration(request.getDuration())
                    .status("DRAFT")
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

        sessionRegulationValidationService.validateDurationByRegulation(
            syllabusId,
            request.getDuration(),
            session.getSessionId()
        );

        session.setSessionNumber(request.getSessionNumber());
        session.setSessionTitle(request.getSessionTitle());
        session.setTeachingMethods(request.getTeachingMethods());
        session.setDuration(request.getDuration());
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
                .duration(session.getDuration())
                .material(new ArrayList<>(materialMap.values()))
                .block(new ArrayList<>(blockMap.values()))
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
}
