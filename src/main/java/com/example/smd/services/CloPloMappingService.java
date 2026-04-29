package com.example.smd.services;

import com.example.smd.dto.request.CloPloMappingBulkRequest;
import com.example.smd.dto.request.CloPloMappingRequest;
import com.example.smd.dto.request.PoPloMappingRequest;
import com.example.smd.dto.response.clo.CloPloMappingBulkResponse;
import com.example.smd.dto.response.clo.CloPloMappingResponse;
import com.example.smd.dto.response.validate.CloPloMappingCheckResponse;
import com.example.smd.dto.response.validate.PoPloMappingCheckResponse;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloPloMappingMapper;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloPloMappingService {

    CloPloMappingRepository repository;
    CloPloMappingMapper mapper;
    CLOsRepository cloRepository;
    PLOsRepository ploRepository;
    SubjectRepository subjectRepository;
    CurriculumRepository curriculumRepository;
    GeminiService geminiService;

    @Transactional
    public CloPloMappingResponse createMapping(CloPloMappingRequest request) {
        if (repository.existsByClo_CloIdAndPlo_PloId(request.getCloId(), request.getPloId())) {
            throw new AppException(ErrorCode.MAPPING_ALREADY_EXISTS);
        }

        CLO_PLO_Mapping mapping = new CLO_PLO_Mapping();
        mapping.setClo(cloRepository.findById(request.getCloId())
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND)));
        mapping.setPlo(ploRepository.findById(request.getPloId())
                .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND)));
        mapping.setContributionLevel(request.getContributionLevel());

        return mapper.toResponse(repository.save(mapping));
    }

    @Transactional
    public List<CloPloMappingResponse> getBySubject(String subjectId) {
        // 1. Kiểm tra Subject có tồn tại không
        if (!subjectRepository.existsById(UUID.fromString(subjectId))) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        // 2. Truy vấn và map sang Response
        return repository.findByClo_Subject_SubjectId(UUID.fromString(subjectId))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloPloMappingResponse> getFullMappingDetails(UUID subjectId, UUID curriculumId) {
        // 1. Lấy dữ liệu Mapping kèm theo CLO và PLO entity
        List<CLO_PLO_Mapping> mappings = repository
                .findMappingsWithDetails(subjectId, curriculumId);

        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Chuyển đổi sang Response DTO (Nơi chứa thông tin cloName, ploCode,...)
        return mappings.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloPloMappingResponse> getByClo(String cloId) {
        // 1. Kiểm tra CLO có tồn tại không
        if (!cloRepository.existsById(UUID.fromString(cloId))) {
            throw new AppException(ErrorCode.CLO_NOT_FOUND);
        }

        // 2. Truy vấn mapping (Session mở nhờ @Transactional nên không bị Lazy error)
        return repository.findByClo_CloId(UUID.fromString(cloId))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CloPloMappingResponse> getByPlo(String ploId) {
        // 1. Kiểm tra PLO có tồn tại không
        if (!ploRepository.existsById(UUID.fromString(ploId))) {
            throw new AppException(ErrorCode.PLO_NOT_FOUND);
        }

        // 2. Truy vấn và map sang Response
        return repository.findByPlo_PloId(UUID.fromString(ploId))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public CloPloMappingResponse updateLevel(String id, String newLevel) {
        CLO_PLO_Mapping mapping = repository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CLO_PLO_MAPPING_NOT_FOUND));
        mapping.setContributionLevel(newLevel);
        return mapper.toResponse(repository.save(mapping));
    }

    @Transactional
    public void deleteMapping(String id) {
        repository.deleteById(UUID.fromString(id));
    }

    @Transactional
    public CloPloMappingBulkResponse bulkConfigureMappings(CloPloMappingBulkRequest request) {
        List<String> warnings = new ArrayList<>();
        int deletedCount = 0;
        int addedCount = 0;

        if (request.getDeletedMappings() != null) {
            for (CloPloMappingRequest item : request.getDeletedMappings()) {
                int affectedRows = repository.deleteByClo_CloIdAndPlo_PloId(item.getCloId(), item.getPloId());
                if (affectedRows > 0) {
                    deletedCount += affectedRows;
                } else {
                    warnings.add("Mapping not found for delete: cloId=" + item.getCloId() + ", ploId=" + item.getPloId());
                }
            }
        }

        if (request.getAddedMappings() != null) {
            for (CloPloMappingRequest item : request.getAddedMappings()) {

                if (repository.existsByClo_CloIdAndPlo_PloId(item.getCloId(), item.getPloId())) {
                    warnings.add("Mapping already exists: cloId=" + item.getCloId() + ", ploId=" + item.getPloId());
                    continue;
                }

                CLO_PLO_Mapping mapping = new CLO_PLO_Mapping();
                mapping.setClo(cloRepository.findById(item.getCloId())
                        .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND)));
                mapping.setPlo(ploRepository.findById(item.getPloId())
                        .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND)));
                mapping.setContributionLevel(item.getContributionLevel());

                repository.save(mapping);
                addedCount++;
            }
        }

        return CloPloMappingBulkResponse.builder()
                .deletedCount(deletedCount)
                .addedCount(addedCount)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    public CloPloMappingCheckResponse checkMapping(List<CloPloMappingRequest> request, UUID curriculumId, UUID subjectId) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        List<CLOs> cloList = cloRepository.findBySubject_SubjectId(subject.getSubjectId());
        String userCloList = cloList.stream()
                .map(clo -> clo.getCloCode() + ": " + clo.getDescription())
                .collect(Collectors.joining("\n"));

        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        List<PLOs> ploList = ploRepository.findByCurriculum_CurriculumId(curriculum.getCurriculumId());
        String userPloList = ploList.stream()
                .map(plo -> plo.getPloCode() + ": " + plo.getDescription())
                .collect(Collectors.joining("\n"));

        String currentMapping = buildCurrentMappingForAI(request);
        log.info("Không tìm thấy: " + currentMapping);

        return geminiService.checkPloCloMapping(userPloList, userCloList, currentMapping);
    }

    public String buildCurrentMappingForAI(List<CloPloMappingRequest> requests) {

        // 1. LẤY UUID (Giả sử getCloId() và getPloId() đang trả về UUID)
        Set<UUID> cloIds = requests.stream()
                .map(CloPloMappingRequest::getCloId)
                .collect(Collectors.toSet());

        Set<UUID> ploIds = requests.stream()
                .map(CloPloMappingRequest::getPloId)
                .collect(Collectors.toSet());

        // 2. GỌI DATABASE 1 LẦN
        List<CLOs> closFromDb = cloRepository.findAllById(cloIds);
        List<PLOs> plosFromDb = ploRepository.findAllById(ploIds);

        // 3. TẠO MAP: KEY = UUID String, VALUE = Code String
        Map<String, String> cloIdToCodeMap = closFromDb.stream()
                .collect(Collectors.toMap(clo -> clo.getCloId().toString(), CLOs::getCloCode));

        Map<String, String> ploIdToCodeMap = plosFromDb.stream()
                .collect(Collectors.toMap(plo -> plo.getPloId().toString(), PLOs::getPloCode));

        // 4. GOM NHÓM DỮ LIỆU (Đã sửa lỗi .toString())
        Map<String, List<String>> mappingResult = requests.stream()
                .filter(req -> ploIdToCodeMap.containsKey(req.getPloId().toString())
                        && cloIdToCodeMap.containsKey(req.getCloId().toString()))
                .collect(Collectors.groupingBy(
                        req -> ploIdToCodeMap.get(req.getPloId().toString()),
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
