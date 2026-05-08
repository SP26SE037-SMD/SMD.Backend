package com.example.smd.services;

import com.example.smd.dto.request.taskV2.TaskV2CreateRequest;
import com.example.smd.dto.request.taskV2.TaskV2CreateVPRequest;
import com.example.smd.dto.request.taskV2.TaskV2UpdateRequest;
import com.example.smd.dto.response.TaskV2Response;
import com.example.smd.entities.*;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
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
    private final SubjectRepository subjectRepository;
    private final SyllabusRepository syllabusRepository;
    private final CurriculumRepository curriculumRepository;
    private final DocumentRepository documentRepository;
    private final SprintRepository sprintRepository;
    private final AccountRepository accountRepository;
    private final TaskV2Mapper taskV2Mapper;
    private final AccountService accountService;
    private final CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;


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
        Set<UUID> subjectIds  = new HashSet<>();
        Set<UUID> syllabusIds = new HashSet<>();
        Set<UUID> curriculumIds = new HashSet<>();
        Set<UUID> documentIds = new HashSet<>();

        for (TaskV2 task : tasks) {
            if (task.getTargetId() != null && task.getType() != null) {
                switch (task.getType()) {
                    case "SUBJECT":     subjectIds.add(task.getTargetId());    break;
                    case "SYLLABUS":    syllabusIds.add(task.getTargetId());   break;
                    case "CURRICULUM":  curriculumIds.add(task.getTargetId()); break;
                    case "MAJOR":       documentIds.add(task.getTargetId());   break;
                }
            }
        }

        // Batch fetch
        Map<UUID, Subject> subjectMap = subjectIds.isEmpty() ? Collections.emptyMap() :
                subjectRepository.findAllById(subjectIds).stream().collect(Collectors.toMap(Subject::getSubjectId, s -> s));

        Map<UUID, Syllabus> syllabusMap = syllabusIds.isEmpty() ? Collections.emptyMap() :
                syllabusRepository.findAllById(syllabusIds).stream().collect(Collectors.toMap(Syllabus::getSyllabusId, s -> s));

        Map<UUID, Curriculum> curriculumMap = curriculumIds.isEmpty() ? Collections.emptyMap() :
                curriculumRepository.findAllById(curriculumIds).stream().collect(Collectors.toMap(Curriculum::getCurriculumId, c -> c));

        Map<UUID, Document> documentMap = documentIds.isEmpty() ? Collections.emptyMap() :
                documentRepository.findAllById(documentIds).stream().collect(Collectors.toMap(Document::getDocumentId, d -> d));

        List<TaskV2Response> responses = tasks.stream()
                .map(task -> enrichResponse(taskV2Mapper.toResponse(task), task.getType(), task.getTargetId(),
                        subjectMap, syllabusMap, curriculumMap, documentMap))
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
                case "SUBJECT":
                    subjectRepository.findById(task.getTargetId()).ifPresent(sub ->
                            response.setSubject(buildSubjectDto(sub)));
                    break;
                case "SYLLABUS":
                    syllabusRepository.findById(task.getTargetId()).ifPresent(s -> {
                        response.setSyllabus(TaskV2Response.SyllabusDto.builder()
                                .syllabusId(s.getSyllabusId())
                                .syllabusName(s.getSyllabusName())
                                .build());
                        if (s.getSubject() != null) {
                            response.setSubject(buildSubjectDto(s.getSubject()));
                        }
                    });
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

    /** Enrich response với thông tin subject/syllabus/curriculum/document từ batch map */
    private TaskV2Response enrichResponse(TaskV2Response response, String type, UUID targetId,
                                          Map<UUID, Subject> subjectMap,
                                          Map<UUID, Syllabus> syllabusMap,
                                          Map<UUID, Curriculum> curriculumMap,
                                          Map<UUID, Document> documentMap) {
        if (type == null || targetId == null) return response;

        switch (type) {
            case "SUBJECT":
                if (subjectMap.containsKey(targetId)) {
                    response.setSubject(buildSubjectDto(subjectMap.get(targetId)));
                }
                break;
            case "SYLLABUS":
                if (syllabusMap.containsKey(targetId)) {
                    Syllabus s = syllabusMap.get(targetId);
                    response.setSyllabus(TaskV2Response.SyllabusDto.builder()
                            .syllabusId(s.getSyllabusId())
                            .syllabusName(s.getSyllabusName())
                            .build());
                    if (s.getSubject() != null) {
                        response.setSubject(buildSubjectDto(s.getSubject()));
                    }
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

    /** Build SubjectDto từ Subject entity (bao gồm thông tin department) */
    private TaskV2Response.SubjectDto buildSubjectDto(Subject sub) {
        String deptCode = null;
        String deptName = null;
        if (sub.getDepartment() != null) {
            deptCode = sub.getDepartment().getDepartmentCode();
            deptName = sub.getDepartment().getDepartmentName();
        }
        return TaskV2Response.SubjectDto.builder()
                .subjectId(sub.getSubjectId())
                .subjectCode(sub.getSubjectCode())
                .subjectName(sub.getSubjectName())
                .credits(sub.getCredits())
                .departmentCode(deptCode)
                .departmentName(deptName)
                .build();
    }

    //====================== CREATE BATCH TASKS=====================
    @Transactional
    public boolean createBatch(UUID sprintId, String check) {
        var checkRole = accountService.getAccountById(check);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.HOCFDC.name().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // Batch API still requires sprintId in path for all roles
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        if (sprint.getCurriculum() == null || sprint.getCurriculum().getCurriculumId() == null) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }
        if (sprint.getAccount() == null || sprint.getAccount().getDepartment() == null ||
                sprint.getAccount().getDepartment().getDepartmentId() == null) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        UUID curriculumId = sprint.getCurriculum().getCurriculumId();
        UUID departmentId = sprint.getAccount().getDepartment().getDepartmentId();

        List<Curriculum_Group_Subject> mappings =
                curriculumGroupSubjectRepository.findByCurriculumIdAndDepartmentId(curriculumId, departmentId);

        Set<UUID> subjectIds = new HashSet<>();
        for (Curriculum_Group_Subject mapping : mappings) {
            if (mapping.getSubject() != null && mapping.getSubject().getSubjectId() != null) {
                subjectIds.add(mapping.getSubject().getSubjectId());
            }
        }

        if (subjectIds.isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        // Query 1 lần: lấy tất cả targetId đã tồn tại trong sprint (cả SUBJECT lẫn SYLLABUS)
        Set<UUID> existingTargetIds = taskV2Repository.findAllTargetIdsInSprint(sprintId);

        Account hopdcAccount = accountRepository
                .findByDepartmentAndRoleName(departmentId, RoleName.HOPDC.name())
                .stream()
                .findFirst()
                .orElse(null);

        List<TaskV2> tasksToSave = new ArrayList<>();
        for (UUID subjectId : subjectIds) {

            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

            TaskV2 task = TaskV2.builder()
                    .taskName(subject.getSubjectCode() + " - " + subject.getSubjectName())
                    .description("Create Syllabus and CLOs of " + subject.getSubjectCode())
                    .priority(Priority.HIGH.toString())
                    .build();

            task.setSprint(sprint);
            task.setAction(ActionType.CREATE.name());
            task.setType(TaskType.SUBJECT.name());
            task.setCreatedBy( accountRepository.findById(UUID.fromString(check))
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
            task.setAccount(hopdcAccount);
            task.setTargetId(subject.getSubjectId());
            task.setStatus(TaskStatus.TO_DO.toString());
            var list =
                    syllabusRepository.findBySubject_SubjectIdAndStatus(subjectId, "PUBLISHED");

            Syllabus syllabus = list.isEmpty() ? null : list.get(0);

            if(syllabus != null) {
                if (hopdcAccount == null) {
                    throw new AppException(
                            ErrorCode.ACCOUNT_NOT_FOUND,
                            "No HoPDC account found in this department"
                    );
                }
                task.setType(TaskType.SYLLABUS.name());
                task.setAction(ActionType.UPDATE.name());
                task.setTargetId(syllabus.getSyllabusId());
                task.setPriority("MEDIUM");
                task.setDescription("Mapping CLOs of " + subject.getSubjectCode() + " to new curriculum ");
            }

            // Skip nếu targetId thực tế (subjectId hoặc syllabusId) đã tồn tại trong sprint
            if (existingTargetIds.contains(task.getTargetId())) {
                continue;
            }

            tasksToSave.add(task);
        }

        if (tasksToSave.isEmpty()) {
            throw new AppException(
                    ErrorCode.TASK_LIST_REQUIRED,
                    "No new tasks can be created for this sprint"
            );
        }

        List<TaskV2> savedTasks = taskV2Repository.saveAll(tasksToSave);

        if (sprint.getAccount() != null &&
                sprint.getAccount().getRole() != null &&
                RoleName.HOPDC.name().equals(sprint.getAccount().getRole().getRoleName())) {
            List<String> taskNames = savedTasks.stream().map(TaskV2::getTaskName).toList();
//            log.info("Notification: Tasks " + taskNames + " created" +
//                    " in sprint " + sprint.getSprintName() +
//                    " for HoPDC account: " + sprint.getAccount().getEmail());
        }

        return true;
    }

//     ===================== CREATE BY VP =====================
    @Transactional
    public TaskV2Response createByVP(
            TaskV2CreateVPRequest request,
            String userId) {
        TaskV2 task = taskV2Mapper.requestVPtoEntity(request);

        Account account = accountRepository.findFirstByRole_RoleName(RoleName.HOCFDC.name())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "No account with role HoCFDC found"));
        task.setAccount(account);
        task.setCreatedBy(accountRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Account (createdBy) not found")));
        task.setStatus(TaskStatus.TO_DO.name());
        task = taskV2Repository.save(task);
        return taskV2Mapper.toResponse(task);
    }
}
