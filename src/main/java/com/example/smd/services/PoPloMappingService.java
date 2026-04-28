package com.example.smd.services;

import com.example.smd.dto.request.PoPloMappingBulkRequest;
import com.example.smd.dto.request.PoPloMappingRequest;
import com.example.smd.dto.response.PoPloCurriculumResponse;
import com.example.smd.dto.response.PoPloMappingBulkResponse;
import com.example.smd.dto.response.PoPloMappingResponse;
import com.example.smd.dto.response.validate.MappingCheckResponse;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PoPloMappingMapper;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.PLOsRepository;
import com.example.smd.repositories.POsRepository;
import com.example.smd.repositories.PoPloMappingRepository;
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
public class PoPloMappingService {

    PoPloMappingRepository repository;
    PoPloMappingMapper mapper;
    POsRepository poRepository;
    PLOsRepository ploRepository;
    CurriculumRepository curriculumRepository;
    GeminiService geminiService;

    @Transactional
    public PoPloMappingResponse createMapping(PoPloMappingRequest request) {
        UUID poId = UUID.fromString(request.getPoId());
        UUID ploId = UUID.fromString(request.getPloId());

        // 1. Kiểm tra mapping đã tồn tại chưa
        if (repository.existsByPo_PoIdAndPlo_PloId(poId, ploId)) {
            throw new AppException(ErrorCode.PO_PLO_MAPPING_EXISTS);
        }

        // 2. Tạo entity mới
        PO_PLO_Mapping mapping = new PO_PLO_Mapping();

        mapping.setPo(poRepository.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND)));

        mapping.setPlo(ploRepository.findById(ploId)
                .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND)));

        return mapper.toResponse(repository.save(mapping));
    }

    public List<PoPloCurriculumResponse> getByCurriculum(String curriculumId) {
        UUID id = parseUuid(curriculumId);
        if (!curriculumRepository.existsById(id)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        return repository.findByPlo_Curriculum_CurriculumId(id)
                .stream()
                .map(mapping -> PoPloCurriculumResponse.builder()
                        .poId(mapping.getPo().getPoId().toString())
                        .poCode(mapping.getPo().getPoCode())
                        .descriptionPo(mapping.getPo().getDescription())
                        .ploId(mapping.getPlo().getPloId().toString())
                        .ploCode(mapping.getPlo().getPloCode())
                        .descriptionPlo(mapping.getPlo().getDescription())
                        .build())
                .toList();
    }

    public List<PoPloMappingResponse> getByPo(String poId) {
        UUID id = parseUuid(poId);
        if (!poRepository.existsById(id)) {
            throw new AppException(ErrorCode.PO_NOT_FOUND);
        }

        return repository.findByPo_PoId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<PoPloMappingResponse> getByPlo(String ploId) {
        UUID id = parseUuid(ploId);
        if (!ploRepository.existsById(id)) {
            throw new AppException(ErrorCode.PLO_NOT_FOUND);
        }

        return repository.findByPlo_PloId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMapping(String id) {
        UUID mappingId = parseUuid(id);
        if (!repository.existsById(mappingId)) {
            throw new AppException(ErrorCode.PO_PLO_MAPPING_NOT_FOUND);
        }
        repository.deleteById(mappingId);
    }

    @Transactional
    public PoPloMappingBulkResponse bulkConfigureMappings(PoPloMappingBulkRequest request) {
        List<String> warnings = new ArrayList<>();
        int deletedCount = 0;
        int addedCount = 0;

        if (request.getDeletedMappings() != null) {
            for (PoPloMappingRequest item : request.getDeletedMappings()) {
                UUID poId = parseUuid(item.getPoId());
                UUID ploId = parseUuid(item.getPloId());

                int affectedRows =
                        repository.deleteByPo_PoIdAndPlo_PloId(poId, ploId);
                if (affectedRows > 0) {
                    deletedCount += (int) affectedRows;
                } else {
                    warnings.add("Mapping not found for delete: poId=" + poId + ", ploId=" + ploId);
                }
            }
        }

        if (request.getAddedMappings() != null) {
            for (PoPloMappingRequest item : request.getAddedMappings()) {
                UUID poId = parseUuid(item.getPoId());
                UUID ploId = parseUuid(item.getPloId());

                if (repository.existsByPo_PoIdAndPlo_PloId(poId, ploId)) {
                    warnings.add("Mapping already exists: poId=" + poId + ", ploId=" + ploId);
                    continue;
                }

                PO_PLO_Mapping mapping = new PO_PLO_Mapping();
                mapping.setPo(poRepository.findById(poId)
                        .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND)));
                mapping.setPlo(ploRepository.findById(ploId)
                        .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND)));

                repository.save(mapping);
                addedCount++;
            }
        }

        return PoPloMappingBulkResponse.builder()
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

    public MappingCheckResponse checkMapping(List<PoPloMappingRequest> request, UUID curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        List<PLOs> ploList = ploRepository.findByCurriculum_CurriculumId(curriculumId);
        String userPloList = ploList.stream()
                .map(plo -> plo.getPloCode() + ": " + plo.getDescription())
                .collect(Collectors.joining("\n"));

        List<PO> poList = poRepository.findByMajor_MajorId(curriculum.getMajor().getMajorId());
        String userPoList = poList.stream()
                .map(po -> po.getPoCode() + ": " + po.getDescription())
                .collect(Collectors.joining("\n"));

        String currentMapping = buildCurrentMappingForAI(request);
        log.error("Không tìm thấy: " + currentMapping);

        return geminiService.checkPoPloMapping(userPoList, userPloList, currentMapping);
    }

    public String buildCurrentMappingForAI(List<PoPloMappingRequest> requests) {

        // 1. LẤY STRING TỪ DTO -> ÉP SANG UUID ĐỂ GỌI DB
        Set<UUID> poIds = requests.stream()
                .map(req -> UUID.fromString(req.getPoId()))
                .collect(Collectors.toSet());

        Set<UUID> ploIds = requests.stream()
                .map(req -> UUID.fromString(req.getPloId()))
                .collect(Collectors.toSet());

        // 2. GỌI DATABASE ĐÚNG 1 LẦN BẰNG UUID
        List<PO> posFromDb = poRepository.findAllById(poIds);
        List<PLOs> plosFromDb = ploRepository.findAllById(ploIds);

        // 3. TẠO MAP: ÉP UUID CỦA ENTITY VỀ LẠI STRING !!!
        // Chỗ này cực kỳ quan trọng: thêm .toString() để Key của Map là String
        Map<String, String> poIdToCodeMap = posFromDb.stream()
                .collect(Collectors.toMap(po -> po.getPoId().toString(), PO::getPoCode));

        Map<String, String> ploIdToCodeMap = plosFromDb.stream()
                .collect(Collectors.toMap(plo -> plo.getPloId().toString(), PLOs::getPloCode));

        // 4. GOM NHÓM DỮ LIỆU
        // Lúc này: Request String dò tìm trong Map có Key là String -> Khớp 100%
        Map<String, List<String>> mappingResult = requests.stream()
                .filter(req -> ploIdToCodeMap.containsKey(req.getPloId()) && poIdToCodeMap.containsKey(req.getPoId()))
                .collect(Collectors.groupingBy(
                        req -> ploIdToCodeMap.get(req.getPloId()), // Lấy ra được Mã (Code)
                        Collectors.mapping(req -> poIdToCodeMap.get(req.getPoId()), Collectors.toList()) // Lấy ra được Mã (Code)
                ));

        // 5. CHUYỂN MAP THÀNH CHUỖI JSON ĐỂ BƠM VÀO PROMPT
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mappingResult);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse mapping data cho AI Prompt", e);
        }
    }
}
