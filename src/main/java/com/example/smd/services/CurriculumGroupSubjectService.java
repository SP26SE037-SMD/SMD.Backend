package com.example.smd.services;

import com.example.smd.dto.request.CurriculumGroupSubjectRequest;
import com.example.smd.dto.response.CurriculumGroupSubjectResponse;
import com.example.smd.dto.response.CurriculumSemesterMappingsResponse;
import com.example.smd.dto.response.SubjectSimpleResponse;
import com.example.smd.dto.request.BulkSemesterMappingRequest;
import com.example.smd.dto.response.BulkSemesterMappingResponse;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CurriculumGroupSubjectMapper;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumGroupSubjectService {

    private final CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final CurriculumGroupSubjectMapper mapper;

    /**
     * Tạo mới mapping giữa Curriculum, Group và Subject
     */
    @Transactional
    public CurriculumGroupSubjectResponse createCurriculumGroupSubject(CurriculumGroupSubjectRequest request) {
        log.info("Creating curriculum-group-subject mapping for curriculum: {}, subject: {}",
                 request.getCurriculumId(), request.getSubjectId());

        // 1. Kiểm tra Curriculum tồn tại
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // 2. Kiểm tra Subject tồn tại
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 3. Kiểm tra Group nếu có
        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        }

        // 4. Kiểm tra đã tồn tại mapping chưa
        boolean exists = curriculumGroupSubjectRepository.existsByCurriculumAndSubject(
            request.getCurriculumId(),
            request.getSubjectId()
        );

        if (exists) {
            throw new AppException(ErrorCode.CURRICULUM_GROUP_SUBJECT_ALREADY_EXISTS);
        }

        // 5. Tạo mới entity
        Curriculum_Group_Subject entity = Curriculum_Group_Subject.builder()
                .curriculum(curriculum)
                .group(group)
                .subject(subject)
                .semester(request.getSemester())
                .build();

        // 6. Lưu vào database
        entity = curriculumGroupSubjectRepository.save(entity);
        log.info("Created curriculum-group-subject mapping with ID: {}", entity.getId());

        // 7. Map sang response
        return mapper.toResponse(entity);
    }

    /**
     * Tìm kiếm subjects theo curriculum hoặc group với phân trang
     */
    @Transactional(readOnly = true)
    public Page<SubjectSimpleResponse> searchSubjects(
            String searchType,
            String searchId,
            int page,
            int size,
            String[] sort
    ) {
        log.info("Searching subjects by {}: {}", searchType, searchId);

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
        Page<Curriculum_Group_Subject> ccsPage;

        switch (searchType.toLowerCase()) {
            case "curriculum":
                // Kiểm tra curriculum tồn tại
                if (!curriculumRepository.existsById(searchUUID)) {
                    throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
                }
                ccsPage = curriculumGroupSubjectRepository.findByCurriculumId(searchUUID, pagingSort);
                break;

            case "group":
                // Kiểm tra group tồn tại
                if (!groupRepository.existsById(searchUUID)) {
                    throw new AppException(ErrorCode.GROUP_NOT_FOUND);
                }
                ccsPage = curriculumGroupSubjectRepository.findByGroupId(searchUUID, pagingSort);
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

    /**
     * Bulk configure semester mappings for a curriculum
     * Atomically inserts multiple subject-semester-group mappings
     */
    @Transactional
    public BulkSemesterMappingResponse bulkConfigureSemesterMappings(
            BulkSemesterMappingRequest request) {
        log.info("Starting bulk semester mapping configuration for curriculum: {}",
                 request.getCurriculumId());

        // Initialize response builder
        var responseBuilder = BulkSemesterMappingResponse.builder()
                .curriculumId(request.getCurriculumId())
                .mappingsBySemester(new java.util.HashMap<>())
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>());

        // Phase 1: Validate Curriculum exists
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> {
                    var error = BulkSemesterMappingResponse.MappingError.builder()
                            .errorCode("CURRICULUM_NOT_FOUND")
                            .errorMessage("Curriculum not found")
                            .details("CurriculumId: " + request.getCurriculumId())
                            .build();
                    responseBuilder.success(false).errors(List.of(error));
                    return new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
                });

        List<BulkSemesterMappingResponse.MappingError> validationErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalMappingsCreated = 0;
        var mappingsBySemester = new java.util.HashMap<Integer, Integer>();

        // Phase 2: Validate all mappings before creating any
        java.util.Set<String> processingKeys = new java.util.HashSet<>();

        for (BulkSemesterMappingRequest.SemesterMappingDTO semMapping : request.getSemesterMappings()) {
            for (BulkSemesterMappingRequest.SubjectGroupMappingDTO subMapping : semMapping.getSubjects()) {
                String processingKey = String.valueOf(subMapping.getSubjectId());

                // Check duplicate in same request
                if (!processingKeys.add(processingKey)) {
                    var error = BulkSemesterMappingResponse.MappingError.builder()
                            .errorCode("DUPLICATE_MAPPING_IN_REQUEST")
                            .errorMessage("Duplicate subject found in request for the same curriculum")
                            .semesterNo(semMapping.getSemesterNo())
                            .subjectId(subMapping.getSubjectId())
                            .groupId(subMapping.getGroupId())
                            .build();
                    validationErrors.add(error);
                    continue;
                }

                // Validate Subject
                Subject subject = subjectRepository.findById(subMapping.getSubjectId())
                        .orElseGet(() -> {
                            var error = BulkSemesterMappingResponse.MappingError.builder()
                                    .errorCode("SUBJECT_NOT_FOUND")
                                    .errorMessage("Subject not found")
                                    .semesterNo(semMapping.getSemesterNo())
                                    .subjectId(subMapping.getSubjectId())
                                    .build();
                            validationErrors.add(error);
                            return null;
                        });

                if (subject == null) continue;

                // Validate Group if provided (not null)
                Group group = null;
                if (subMapping.getGroupId() != null) {
                    group = groupRepository.findById(subMapping.getGroupId())
                            .orElseGet(() -> {
                                var error = BulkSemesterMappingResponse.MappingError.builder()
                                        .errorCode("GROUP_NOT_FOUND")
                                        .errorMessage("Group not found")
                                        .semesterNo(semMapping.getSemesterNo())
                                        .groupId(subMapping.getGroupId())
                                        .build();
                                validationErrors.add(error);
                                return null;
                            });

                    if (group == null) continue;

            // Check group thuộc curriculum dựa trên bảng Curriculum_Group_Subject
//            if (!curriculumGroupSubjectRepository.existsByCurriculumAndGroup(
//                request.getCurriculumId(),
//                subMapping.getGroupId())) {
//                        var error = BulkSemesterMappingResponse.MappingError.builder()
//                                .errorCode("GROUP_NOT_IN_CURRICULUM")
//                                .errorMessage("Group does not belong to this curriculum")
//                                .semesterNo(semMapping.getSemesterNo())
//                                .groupId(subMapping.getGroupId())
//                                .build();
//                        validationErrors.add(error);
//                        continue;
//                    }
                }

                // Check if mapping already exists
                boolean exists = curriculumGroupSubjectRepository.existsByCurriculumAndSubject(
                    request.getCurriculumId(),
                    subMapping.getSubjectId()
                );

                if (exists) {
                    var error = BulkSemesterMappingResponse.MappingError.builder()
                        .errorCode("CURRICULUM_GROUP_SUBJECT_ALREADY_EXISTS")
                        .errorMessage("Subject already exists in this curriculum")
                        .semesterNo(semMapping.getSemesterNo())
                        .subjectId(subMapping.getSubjectId())
                        .groupId(subMapping.getGroupId())
                        .build();
                    validationErrors.add(error);
                }
            }
        }

        // If there are validation errors, return early
        if (!validationErrors.isEmpty()) {
            log.warn("Validation errors found during bulk mapping: {} errors", validationErrors.size());
            return responseBuilder
                    .success(false)
                    .errors(validationErrors)
                    .warnings(warnings)
                    .build();
        }

        // Phase 3: Create and save mappings (Transaction guaranteed)
        try {
            List<Curriculum_Group_Subject> entitiesToSave = new ArrayList<>();

            for (BulkSemesterMappingRequest.SemesterMappingDTO semMapping : request.getSemesterMappings()) {
                for (BulkSemesterMappingRequest.SubjectGroupMappingDTO subMapping : semMapping.getSubjects()) {
                    // Skip if subject or group validation failed
                    if (!subjectRepository.existsById(subMapping.getSubjectId())) {
                        continue;
                    }

                    if (subMapping.getGroupId() != null &&
                        !groupRepository.existsById(subMapping.getGroupId())) {
                        continue;
                    }

                    Subject subject = subjectRepository.getReferenceById(subMapping.getSubjectId());
                    Group group = subMapping.getGroupId() != null ?
                                  groupRepository.getReferenceById(subMapping.getGroupId()) : null;

                    Curriculum_Group_Subject entity = Curriculum_Group_Subject.builder()
                            .curriculum(curriculum)
                            .semester(semMapping.getSemesterNo())
                            .subject(subject)
                            .group(group)
                            .build();

                    entitiesToSave.add(entity);

                    // Update semester mapping counter
                    mappingsBySemester.merge(semMapping.getSemesterNo(), 1, Integer::sum);
                    totalMappingsCreated++;
                }
            }

            // Save all at once
            curriculumGroupSubjectRepository.saveAll(entitiesToSave);
            log.info("Successfully created {} curriculum-group-subject mappings for curriculum: {}",
                     totalMappingsCreated, request.getCurriculumId());

            return responseBuilder
                    .success(true)
                    .totalMappingsCreated(totalMappingsCreated)
                    .totalSemestersMapped(mappingsBySemester.size())
                    .mappingsBySemester(mappingsBySemester)
                    .warnings(warnings.isEmpty() ? null : warnings)
                    .build();

        } catch (Exception e) {
            log.error("Error during bulk mapping insertion: {}", e.getMessage(), e);
            var error = BulkSemesterMappingResponse.MappingError.builder()
                    .errorCode("DATABASE_ERROR")
                    .errorMessage("Error during database insertion")
                    .details(e.getMessage())
                    .build();
            return responseBuilder
                    .success(false)
                    .errors(List.of(error))
                    .build();
        }
    }

    /**
     * Bulk update-like configure for semester mappings.
     * Rule: duplicate subject in the same curriculum is only inserted when groupId is not null.
     * If duplicate subject has null groupId, skip that item and continue processing remaining items.
     */
    @Transactional
    public BulkSemesterMappingResponse bulkConfigureSemesterMappingsPut(BulkSemesterMappingRequest request) {
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        List<String> warnings = new ArrayList<>();
        List<BulkSemesterMappingResponse.MappingError> errors = new ArrayList<>();
        int totalMappingsCreated = 0;
        Map<Integer, Integer> mappingsBySemester = new java.util.HashMap<>();

        // Track null-group duplicates inside the same request
        java.util.Set<String> nullGroupProcessingKeys = new java.util.HashSet<>();
        List<Curriculum_Group_Subject> entitiesToSave = new ArrayList<>();

        for (BulkSemesterMappingRequest.SemesterMappingDTO semMapping : request.getSemesterMappings()) {
            for (BulkSemesterMappingRequest.SubjectGroupMappingDTO subMapping : semMapping.getSubjects()) {
                UUID subjectId = subMapping.getSubjectId();
                UUID groupId = subMapping.getGroupId();

                Subject subject = subjectRepository.findById(subjectId)
                        .orElseGet(() -> {
                            errors.add(BulkSemesterMappingResponse.MappingError.builder()
                                    .errorCode("SUBJECT_NOT_FOUND")
                                    .errorMessage("Subject not found")
                                    .semesterNo(semMapping.getSemesterNo())
                                    .subjectId(subjectId)
                                    .groupId(groupId)
                                    .build());
                            return null;
                        });
                if (subject == null) {
                    continue;
                }

                Group group = null;
                if (groupId != null) {
                    group = groupRepository.findById(groupId)
                            .orElseGet(() -> {
                                errors.add(BulkSemesterMappingResponse.MappingError.builder()
                                        .errorCode("GROUP_NOT_FOUND")
                                        .errorMessage("Group not found")
                                        .semesterNo(semMapping.getSemesterNo())
                                        .subjectId(subjectId)
                                        .groupId(groupId)
                                        .build());
                                return null;
                            });
                    if (group == null) {
                        continue;
                    }
                }

                boolean duplicateSubjectInCurriculum =
                        curriculumGroupSubjectRepository.existsByCurriculumAndSubject(request.getCurriculumId(), subjectId);

                // If duplicate subject and groupId is null => skip this item and continue
                if (duplicateSubjectInCurriculum && groupId == null) {
                    warnings.add("Skipped duplicate subject without group: " + subjectId +
                            " in semester: " + semMapping.getSemesterNo());
                    continue;
                }

                // Prevent exact duplicate mapping regardless of mode
                boolean exactExists = curriculumGroupSubjectRepository.existsByCurriculumAndSubjectAndGroup(
                        request.getCurriculumId(),
                        subjectId,
                        groupId
                );
                if (exactExists) {
                    warnings.add("Skipped exact existing mapping for subject: " + subjectId +
                            ", group: " + groupId + ", semester: " + semMapping.getSemesterNo());
                    continue;
                }

                // For null-group, skip duplicate entries inside current request
                if (groupId == null) {
                    String nullGroupKey = subjectId.toString();
                    if (!nullGroupProcessingKeys.add(nullGroupKey)) {
                        warnings.add("Skipped duplicate null-group subject in request: " + subjectId +
                                " in semester: " + semMapping.getSemesterNo());
                        continue;
                    }
                }

                Curriculum_Group_Subject entity = Curriculum_Group_Subject.builder()
                        .curriculum(curriculum)
                        .semester(semMapping.getSemesterNo())
                        .subject(subject)
                        .group(group)
                        .build();

                entitiesToSave.add(entity);
                mappingsBySemester.merge(semMapping.getSemesterNo(), 1, Integer::sum);
                totalMappingsCreated++;
            }
        }

        if (!entitiesToSave.isEmpty()) {
            curriculumGroupSubjectRepository.saveAll(entitiesToSave);
g        }

        return BulkSemesterMappingResponse.builder()
                .success(errors.isEmpty())
                .curriculumId(request.getCurriculumId())
                .totalMappingsCreated(totalMappingsCreated)
                .totalSemestersMapped(mappingsBySemester.size())
                .mappingsBySemester(mappingsBySemester)
                .errors(errors)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
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

    @Transactional(readOnly = true)
    public CurriculumSemesterMappingsResponse getSemesterMappingsByCurriculum(String curriculumId) {
        UUID curriculumUUID;
        try {
            curriculumUUID = UUID.fromString(curriculumId);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (!curriculumRepository.existsById(curriculumUUID)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        List<Curriculum_Group_Subject> mappings =
                curriculumGroupSubjectRepository.findAllByCurriculumIdOrderBySemester(curriculumUUID);

        Map<Integer, List<Curriculum_Group_Subject>> groupedBySemester = new LinkedHashMap<>();
        for (Curriculum_Group_Subject mapping : mappings) {
            Integer semesterNo = mapping.getSemester();
            groupedBySemester
                    .computeIfAbsent(semesterNo, key -> new ArrayList<>())
                    .add(mapping);
        }

        List<CurriculumSemesterMappingsResponse.SemesterMappingItem> semesterMappings = new ArrayList<>();
        for (Map.Entry<Integer, List<Curriculum_Group_Subject>> entry : groupedBySemester.entrySet()) {
            List<CurriculumSemesterMappingsResponse.SubjectMappingItem> subjects =
                    entry.getValue().stream()
                            .map(mapping -> {
                                Subject subject = mapping.getSubject();
                                Group group = mapping.getGroup();
                                return CurriculumSemesterMappingsResponse.SubjectMappingItem.builder()
                                        .subjectId(subject.getSubjectId())
                                        .subjectCode(subject.getSubjectCode())
                                        .subjectName(subject.getSubjectName())
                                        .groupId(group != null ? group.getGroupId() : null)
                                        .build();
                            })
                            .sorted(Comparator.comparing(CurriculumSemesterMappingsResponse.SubjectMappingItem::getSubjectCode,
                                    Comparator.nullsLast(String::compareToIgnoreCase)))
                            .toList();

            semesterMappings.add(CurriculumSemesterMappingsResponse.SemesterMappingItem.builder()
                    .semesterNo(entry.getKey())
                    .subjects(subjects)
                    .build());
        }

        return CurriculumSemesterMappingsResponse.builder()
                .curriculumId(curriculumUUID)
                .semesterMappings(semesterMappings)
                .build();
    }
}
