package com.example.smd.services;

import com.example.smd.dto.request.CurriculumComboSubjectRequest;
import com.example.smd.dto.response.CurriculumComboSubjectResponse;
import com.example.smd.dto.response.SubjectSimpleResponse;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CurriculumComboSubjectMapper;
import com.example.smd.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumComboSubjectService {

    private final CurriculumComboSubjectRepository curriculumComboSubjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final ComboRepository comboRepository;
    private final SubjectRepository subjectRepository;
    private final CurriculumComboSubjectMapper mapper;

    /**
     * Tạo mới mapping giữa Curriculum, Combo và Subject
     */
    @Transactional
    public CurriculumComboSubjectResponse createCurriculumComboSubject(CurriculumComboSubjectRequest request) {
        log.info("Creating curriculum-combo-subject mapping for curriculum: {}, subject: {}",
                 request.getCurriculumId(), request.getSubjectId());

        // 1. Kiểm tra Curriculum tồn tại
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // 2. Kiểm tra Subject tồn tại
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 3. Kiểm tra Combo nếu có
        Combo combo = null;
        if (request.getComboId() != null) {
            combo = comboRepository.findById(request.getComboId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
        }

        // 4. Kiểm tra đã tồn tại mapping chưa
        boolean exists = curriculumComboSubjectRepository.existsByCurriculumAndSubjectAndCombo(
                request.getCurriculumId(),
                request.getSubjectId(),
                request.getComboId()
        );

        if (exists) {
            throw new AppException(ErrorCode.CURRICULUM_COMBO_SUBJECT_ALREADY_EXISTS);
        }

        // 5. Tạo mới entity
        Curriculum_Combo_Subject entity = Curriculum_Combo_Subject.builder()
                .curriculum(curriculum)
                .combo(combo)
                .subject(subject)
                .semester(request.getSemester())
                .build();

        // 6. Lưu vào database
        entity = curriculumComboSubjectRepository.save(entity);
        log.info("Created curriculum-combo-subject mapping with ID: {}", entity.getId());

        // 7. Map sang response
        return mapper.toResponse(entity);
    }

    /**
     * Tìm kiếm subjects theo curriculum hoặc combo với phân trang
     */
    @Transactional(readOnly = true)
    public Page<SubjectSimpleResponse> searchSubjects(
            String search,
            String searchType,
            String searchId,
            int page,
            int size,
            String[] sort
    ) {
        log.info("Searching subjects by {}: {} with search term: {}", searchType, searchId, search);

        // 1. Xử lý sắp xếp
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        // 2. Parse searchId
        UUID searchUUID;
        try {
            searchUUID = UUID.fromString(searchId);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // 3. Tìm kiếm theo searchType
        Page<Curriculum_Combo_Subject> ccsPage;
        String searchTerm = (search == null || search.trim().isEmpty()) ? null : search.trim();

        switch (searchType.toLowerCase()) {
            case "curriculum":
                // Kiểm tra curriculum tồn tại
                if (!curriculumRepository.existsById(searchUUID)) {
                    throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
                }
                ccsPage = curriculumComboSubjectRepository.findByCurriculumWithSearch(
                    searchUUID, searchTerm, pagingSort
                );
                break;

            case "combo":
                // Kiểm tra combo tồn tại
                if (!comboRepository.existsById(searchUUID)) {
                    throw new AppException(ErrorCode.COMBO_NOT_FOUND);
                }
                ccsPage = curriculumComboSubjectRepository.findByComboWithSearch(
                    searchUUID, searchTerm, pagingSort
                );
                break;

            default:
                throw new AppException(ErrorCode.INVALID_KEY);
        }

        // 4. Map sang SubjectSimpleResponse với semester
        return ccsPage.map(ccs -> {
            Subject subject = ccs.getSubject();
            return SubjectSimpleResponse.builder()
                    .subjectId(subject.getSubjectId())
                    .subjectCode(subject.getSubjectCode())
                    .subjectName(subject.getSubjectName())
                    .credits(subject.getCredits())
                    .semester(ccs.getSemester())
                    .build();
        });
    }

    // Helper method để xử lý hướng sắp xếp
    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
