package com.example.smd.services;

import com.example.smd.dto.request.PoPloMappingBulkRequest;
import com.example.smd.dto.request.PoPloMappingRequest;
import com.example.smd.dto.response.PoPloCurriculumResponse;
import com.example.smd.dto.response.PoPloMappingBulkResponse;
import com.example.smd.dto.response.PoPloMappingResponse;
import com.example.smd.entities.PO_PLO_Mapping;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PoPloMappingMapper;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.PLOsRepository;
import com.example.smd.repositories.POsRepository;
import com.example.smd.repositories.PoPloMappingRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

                long affectedRows = repository.deleteByPo_PoIdAndPlo_PloId(poId, ploId);
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
}
