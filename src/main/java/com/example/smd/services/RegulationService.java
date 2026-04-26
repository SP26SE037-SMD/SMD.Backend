package com.example.smd.services;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.RegulationMapper;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegulationService {

    private final RegulationRepository regulationRepository;
    private final RegulationMapper regulationMapper;
    private final MajorRepository majorRepository;

    @Transactional(readOnly = true)
    public Page<RegulationResponse> getAll(String search, int page, int size, String[] sort) {
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
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            );
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

        if (request.getCode() != null &&
                regulationRepository.existsByCodeAndRegulationIdNot(request.getCode(), id)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Regulation code already exists");
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
}
