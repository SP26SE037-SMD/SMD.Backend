package com.example.smd.services;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.validate.ProgramRegulationResponse;
import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.mapper.RegulationMapper;
import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegulationService {

    private final RegulationRepository regulationRepository;
    private final RegulationMapper regulationMapper;
    private final MajorRepository majorRepository;
    private final RegulationAsyncService regulationAsyncService;

    @Transactional(readOnly = true)
    public Page<RegulationResponse> getAll(String search, int page, int size, String[] sort, UUID majorId) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] split = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(split[1]), split[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        Specification<Regulation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc bắt buộc theo majorId
            if (majorId != null) {
                predicates.add(cb.equal(root.get("major").get("majorId"), majorId));
            }

            // 2. Lọc theo từ khóa search (nếu có)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
                );
                predicates.add(searchPredicate);
            }

            // Kết hợp các điều kiện bằng phép AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return regulationRepository.findAll(spec, pageable)
                .map(regulationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RegulationResponse getById(UUID id) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public RegulationResponse create(RegulationRequest request) {
        if (regulationRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.INVALID_KEY, "Regulation code already exists");
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        Regulation regulation = regulationMapper.toEntity(request);
        regulation.setMajor(major);
        regulation = regulationRepository.save(regulation);
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public RegulationResponse update(UUID id, RegulationRequest request) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));

        UUID currentMajorId = request.getMajorId() != null
                ? request.getMajorId()
                : regulation.getMajor().getMajorId();
        if (request.getCode() != null &&
                regulationRepository.existsByCodeAndMajor_MajorIdAndRegulationIdNot(request.getCode(), currentMajorId, id)) {
            throw new AppException(ErrorCode.INVALID_KEY, "This Regulation code already exists within the current major!");
        }

        regulationMapper.updateEntity(regulation, request);
        regulation = regulationRepository.save(regulation);
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public boolean delete(UUID id) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));
        regulationRepository.delete(regulation);
        return true;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    public String startImportProcess(MultipartFile file, String accountId) throws IOException {
        // Gửi ngay phản hồi cho người dùng
        byte[] fileData = file.getBytes();
        String contentType = file.getContentType();
        regulationAsyncService.importMajorAndAddRegulation(fileData, contentType, accountId);
        return "The system is processing the file, please wait for a notification!";
    }
}
