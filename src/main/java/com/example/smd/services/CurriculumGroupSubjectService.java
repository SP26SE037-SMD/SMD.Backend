package com.example.smd.services;

import com.example.smd.dto.request.CurriculumGroupSubjectRequest;
import com.example.smd.dto.response.CurriculumGroupSubjectResponse;
import com.example.smd.dto.response.CurriculumSemesterMappingsResponse;
import com.example.smd.dto.response.SubjectSimpleResponse;
import com.example.smd.dto.request.BulkSemesterMappingRequest;
import com.example.smd.dto.response.BulkSemesterMappingResponse;
import com.example.smd.entities.*;
import com.example.smd.enums.CurriculumStatus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SubjectStatus;
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
import java.util.Collections;
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
    private final AccountService accountService;

    /**
     * Tạo mới mapping giữa Curriculum, Group và Subject
     */
    @Transactional
    public CurriculumGroupSubjectResponse createCurriculumGroupSubject(CurriculumGroupSubjectRequest request, String accountId) {
        log.info("Creating curriculum-group-subject mapping for curriculum: {}, subject: {}",
                 request.getCurriculumId(), request.getSubjectId());

        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!"HOCFDC".equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Kiểm tra Curriculum tồn tại
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // 2. Kiểm tra Subject tồn tại
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!(CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus()) && SubjectStatus.DRAFT.toString().equals(subject.getStatus()))) {
            throw new AppException(ErrorCode.CURRICULUM_GROUP_SUBJECT_NOT_CREATE);
        }

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
     * Bulk update-like configure for semester mappings.
     * Rule: duplicate subject in the same curriculum is only inserted when groupId is not null.
     * If duplicate subject has null groupId, skip that item and continue processing remaining items.
     */
    @Transactional
    public BulkSemesterMappingResponse bulkConfigureSemesterMappingsPut(BulkSemesterMappingRequest request) {
        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        if (request.getDeleteSubjectsList() != null && !request.getDeleteSubjectsList().isEmpty()) {
            curriculumGroupSubjectRepository.deleteByCurriculumIdAndSubjectIds(
                request.getCurriculumId(),
                request.getDeleteSubjectsList()
            );
        }

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
        }

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

    @Transactional(readOnly = true)
    public List<SubjectSimpleResponse> getSubjectsByCurriculumStatusAndDepartment(
            String curriculumId,
            String status,
            String departmentId
    ) {
        UUID curriculumUUID;
        UUID departmentUUID;

        try {
            curriculumUUID = UUID.fromString(curriculumId);
            departmentUUID = UUID.fromString(departmentId);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Curriculum curriculum = curriculumRepository.findById(curriculumUUID)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        if (status != null && !status.isBlank() &&
                (curriculum.getStatus() == null || !curriculum.getStatus().equalsIgnoreCase(status.trim()))) {
            return Collections.emptyList();
        }

        List<Curriculum_Group_Subject> mappings =
                curriculumGroupSubjectRepository.findByCurriculumIdAndDepartmentId(curriculumUUID, departmentUUID);

        return mappings.stream()
                .map(ccs -> {
                    Subject subject = ccs.getSubject();
                    return SubjectSimpleResponse.builder()
                            .subjectId(subject.getSubjectId())
                            .subjectCode(subject.getSubjectCode())
                            .subjectName(subject.getSubjectName())
                            .status(subject.getStatus())
                            .semester(ccs.getSemester())
                            .build();
                })
                .toList();
    }
}
