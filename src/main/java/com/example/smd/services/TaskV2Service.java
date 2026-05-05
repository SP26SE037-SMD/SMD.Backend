package com.example.smd.services;

import com.example.smd.dto.request.taskV2.TaskV2CreateRequest;
import com.example.smd.dto.request.taskV2.TaskV2UpdateRequest;
import com.example.smd.dto.response.TaskV2Response;
import com.example.smd.entities.*;
import com.example.smd.mapper.TaskV2Mapper;
import com.example.smd.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskV2Service {

    private final TaskV2Repository taskV2Repository;
    private final SyllabusRepository syllabusRepository;
    private final CurriculumRepository curriculumRepository;
    private final DocumentRepository documentRepository;
    private final SprintRepository sprintRepository;
    private final AccountRepository accountRepository;
    private final TaskV2Mapper taskV2Mapper;

    // ===================== GET ALL =====================

    @Transactional(readOnly = true)
    public Page<TaskV2Response> getAllTasks(String search, String status, UUID sprintId, String type, String action,
                                           UUID assignTo, UUID createdBy, UUID targetId, Pageable pageable) {
        Specification<TaskV2> spec = TaskV2Specification.withFilters(search, status, sprintId, type, action, assignTo, createdBy, targetId);
        Page<TaskV2> taskPage = taskV2Repository.findAll(spec, pageable);

        List<TaskV2> tasks = taskPage.getContent();
        if (tasks.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, taskPage.getTotalElements());
        }

        // Collect targetIds by type for batch loading
        Set<UUID> syllabusIds = new HashSet<>();
        Set<UUID> curriculumIds = new HashSet<>();
        Set<UUID> documentIds = new HashSet<>();

        for (TaskV2 task : tasks) {
            if (task.getTargetId() != null && task.getType() != null) {
                switch (task.getType()) {
                    case "SYLLABUS":    syllabusIds.add(task.getTargetId());   break;
                    case "CURRICULUM":  curriculumIds.add(task.getTargetId()); break;
                    case "MAJOR":       documentIds.add(task.getTargetId());   break;
                }
            }
        }

        // Batch fetch
        Map<UUID, Syllabus> syllabusMap = syllabusIds.isEmpty() ? Collections.emptyMap() :
                syllabusRepository.findAllById(syllabusIds).stream().collect(Collectors.toMap(Syllabus::getSyllabusId, s -> s));

        Map<UUID, Curriculum> curriculumMap = curriculumIds.isEmpty() ? Collections.emptyMap() :
                curriculumRepository.findAllById(curriculumIds).stream().collect(Collectors.toMap(Curriculum::getCurriculumId, c -> c));

        Map<UUID, Document> documentMap = documentIds.isEmpty() ? Collections.emptyMap() :
                documentRepository.findAllById(documentIds).stream().collect(Collectors.toMap(Document::getDocumentId, d -> d));

        List<TaskV2Response> responses = tasks.stream()
                .map(task -> enrichResponse(taskV2Mapper.toResponse(task), task.getType(), task.getTargetId(),
                        syllabusMap, curriculumMap, documentMap))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, taskPage.getTotalElements());
    }

    // ===================== GET BY ID =====================

    @Transactional(readOnly = true)
    public TaskV2Response getTaskById(UUID taskId) {
        TaskV2 task = taskV2Repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        TaskV2Response response = taskV2Mapper.toResponse(task);

        if (task.getTargetId() != null && task.getType() != null) {
            switch (task.getType()) {
                case "SYLLABUS":
                    syllabusRepository.findById(task.getTargetId()).ifPresent(s ->
                            response.setSyllabus(TaskV2Response.SyllabusDto.builder()
                                    .syllabusId(s.getSyllabusId())
                                    .syllabusName(s.getSyllabusName())
                                    .build()));
                    break;
                case "CURRICULUM":
                    curriculumRepository.findById(task.getTargetId()).ifPresent(c ->
                            response.setCurriculum(TaskV2Response.CurriculumDto.builder()
                                    .curriculumId(c.getCurriculumId())
                                    .curriculumCode(c.getCurriculumCode())
                                    .curriculumName(c.getCurriculumName())
                                    .build()));
                    break;
                case "MAJOR":
                    documentRepository.findById(task.getTargetId()).ifPresent(d ->
                            response.setDocument(TaskV2Response.DocumentDto.builder()
                                    .documentId(d.getDocumentId())
                                    .documentUrl(d.getDocumentUrl())
                                    .build()));
                    break;
            }
        }

        return response;
    }

    // ===================== CREATE =====================

    @Transactional
    public TaskV2Response createTask(TaskV2CreateRequest request, String userId) {
        TaskV2 task = taskV2Mapper.toEntity(request);
        task.setStatus("TO_DO");
        task.setCreatedBy(accountRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Account (createdBy) not found")));

        if (request.getSprintId() != null) {
            task.setSprint(sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint not found")));
        }

        if (request.getAssignTo() != null) {
            task.setAccount(accountRepository.findById(request.getAssignTo())
                    .orElseThrow(() -> new RuntimeException("Account (assignTo) not found")));
        }

        return getTaskById(taskV2Repository.save(task).getTaskId());
    }

    // ===================== UPDATE =====================

    @Transactional
    public TaskV2Response updateTask(UUID taskId, TaskV2UpdateRequest request) {
        TaskV2 task = taskV2Repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        taskV2Mapper.updateEntity(task, request);

        if (request.getAssignTo() != null) {
            task.setAccount(accountRepository.findById(request.getAssignTo())
                    .orElseThrow(() -> new RuntimeException("Account (assignTo) not found")));
        } else {
            task.setAccount(null);
        }

        applyCompletedAt(task);

        return getTaskById(taskV2Repository.save(task).getTaskId());
    }

    // ===================== UPDATE STATUS =====================

    @Transactional
    public TaskV2Response updateTaskStatus(UUID taskId,
                                           String request) {
        TaskV2 task = taskV2Repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (request != null) {
            task.setStatus(request);
        }

        applyCompletedAt(task);

        return getTaskById(taskV2Repository.save(task).getTaskId());
    }

    // ===================== DELETE =====================

    @Transactional
    public void deleteTask(UUID taskId) {
        if (!taskV2Repository.existsById(taskId)) {
            throw new RuntimeException("Task not found");
        }
        taskV2Repository.deleteById(taskId);
    }

    // ===================== PRIVATE HELPERS =====================

    /** Tự động set/xóa completedAt theo status DONE */
    private void applyCompletedAt(TaskV2 task) {
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDate.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }

    /** Enrich response với thông tin syllabus/curriculum/document từ batch map */
    private TaskV2Response enrichResponse(TaskV2Response response, String type, UUID targetId,
                                          Map<UUID, Syllabus> syllabusMap,
                                          Map<UUID, Curriculum> curriculumMap,
                                          Map<UUID, Document> documentMap) {
        if (type == null || targetId == null) return response;

        switch (type) {
            case "SYLLABUS":
                if (syllabusMap.containsKey(targetId)) {
                    Syllabus s = syllabusMap.get(targetId);
                    response.setSyllabus(TaskV2Response.SyllabusDto.builder()
                            .syllabusId(s.getSyllabusId())
                            .syllabusName(s.getSyllabusName())
                            .build());
                }
                break;
            case "CURRICULUM":
                if (curriculumMap.containsKey(targetId)) {
                    Curriculum c = curriculumMap.get(targetId);
                    response.setCurriculum(TaskV2Response.CurriculumDto.builder()
                            .curriculumId(c.getCurriculumId())
                            .curriculumCode(c.getCurriculumCode())
                            .curriculumName(c.getCurriculumName())
                            .build());
                }
                break;
            case "MAJOR":
                if (documentMap.containsKey(targetId)) {
                    Document d = documentMap.get(targetId);
                    response.setDocument(TaskV2Response.DocumentDto.builder()
                            .documentId(d.getDocumentId())
                            .documentUrl(d.getDocumentUrl())
                            .build());
                }
                break;
        }

        return response;
    }
}
