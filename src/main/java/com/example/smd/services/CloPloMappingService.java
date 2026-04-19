package com.example.smd.services;

import com.example.smd.dto.request.CloPloMappingBulkRequest;
import com.example.smd.dto.request.CloPloMappingRequest;
import com.example.smd.dto.response.clo.CloPloMappingBulkResponse;
import com.example.smd.dto.response.clo.CloPloMappingResponse;
import com.example.smd.entities.CLO_PLO_Mapping;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CloPloMappingMapper;
import com.example.smd.repositories.CLOsRepository;
import com.example.smd.repositories.CloPloMappingRepository;
import com.example.smd.repositories.PLOsRepository;
import com.example.smd.repositories.SubjectRepository;
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
public class CloPloMappingService {

    CloPloMappingRepository repository;
    CloPloMappingMapper mapper;
    CLOsRepository cloRepository;
    PLOsRepository ploRepository;
    SubjectRepository subjectRepository;

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
}
