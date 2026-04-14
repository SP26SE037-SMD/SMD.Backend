package com.example.smd.services;

import com.example.smd.dto.request.task.TaskCreateRequest;
import com.example.smd.dto.request.task.TaskUpdateRequest;
import com.example.smd.dto.response.task.TaskListResponse;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Sprint;
import com.example.smd.entities.Curriculum_Group_Subject;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Task;
import com.example.smd.enums.Priority;
import com.example.smd.enums.RoleName;
import com.example.smd.enums.TaskStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.TaskMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.CurriculumGroupSubjectRepository;
import com.example.smd.repositories.SprintRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.repositories.TaskRepository;
import com.example.smd.repositories.TaskSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskService {
    TaskRepository taskRepository;
    SprintRepository sprintRepository;
    AccountRepository accountRepository;
    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;
    AccountService accountService;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(TaskCreateRequest request, String check) {
        var checkRole = accountService.getAccountById(check);
        String roleName = checkRole.getRole().getRoleName();
        if (!( RoleName.HOCFDC.name().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Task task = taskMapper.toTask(request);

        if (request.getSprintId() == null) {
            throw new AppException(ErrorCode.SPRINT_ID_REQUIRED);
        }
        Sprint sprint = sprintRepository.findById(request.getSprintId())
            .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
        task.setSprint(sprint);


        // Map Subject if provided
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
            task.setSubject(subject);
        }

        task.setStatus(TaskStatus.TO_DO.name());

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

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

        long existingTaskCount = taskRepository.countBySprint_SprintId(sprintId);
        if (existingTaskCount >= subjectIds.size()) {
            throw new AppException(
                ErrorCode.TASK_LIST_REQUIRED,
                "Task list for this sprint has already reached the number of subjects in department"
            );
        }

        Set<UUID> existingSubjectIds = taskRepository.findExistingSubjectIdsInSprint(sprintId, subjectIds);

        Account hopdcAccount = accountRepository
            .findByDepartmentAndRoleName(departmentId, RoleName.HOPDC.name())
            .stream()
            .findFirst()
            .orElse(null);

        List<Task> tasksToSave = new ArrayList<>();
        for (UUID subjectId : subjectIds) {
            if (existingSubjectIds.contains(subjectId)) {
                continue;
            }

            Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

            boolean hasSyllabus = !syllabusRepository.findBySubject_SubjectId(subjectId).isEmpty();
            Task task = Task.builder()
                .taskName(subject.getSubjectCode() + " - " + subject.getSubjectName())
                .description("Auto-generated task for subject " + subject.getSubjectCode())
                .priority(Priority.HIGH.toString())
                .build();

            task.setSprint(sprint);
            task.setSubject(subject);
            task.setStatus(TaskStatus.TO_DO.toString());
            var syllabus =
                    syllabusRepository.findBySubject_SubjectIdAndStatus(subjectId, "PUBLISHED");

            if(syllabus != null) {
                if (hopdcAccount == null) {
                    throw new AppException(
                            ErrorCode.ACCOUNT_NOT_FOUND,
                            "No HoPDC account found in this department"
                    );
                }
                task.setType("REUSED_SUBJECT");
                task.setSyllabus(syllabus.get(0)); // Assuming the first syllabus is the one to link
                task.setAccount(hopdcAccount);
            }else {
                task.setType("NEW_SUBJECT");
                task.setAccount(null);
            }
            tasksToSave.add(task);
        }

        if (tasksToSave.isEmpty()) {
            throw new AppException(
                ErrorCode.TASK_LIST_REQUIRED,
                "No new tasks can be created for this sprint"
            );
        }

        List<Task> savedTasks = taskRepository.saveAll(tasksToSave);

        if (sprint.getAccount() != null &&
            sprint.getAccount().getRole() != null &&
            RoleName.HOPDC.name().equals(sprint.getAccount().getRole().getRoleName())) {
            List<String> taskNames = savedTasks.stream().map(Task::getTaskName).toList();
            log.info("Notification: Tasks " + taskNames + " created" +
                " in sprint " + sprint.getSprintName() +
                " for HoPDC account: " + sprint.getAccount().getEmail());
        }

//        return savedTasks.stream().map(taskMapper::toTaskResponse).toList();
        return true;
    }

    public Page<TaskListResponse> getAll(String search, String status, UUID sprintId, UUID accountId, UUID departmentId, UUID syllabusId, Pageable pageable) {
        var spec = TaskSpecification.withFilters(search, status, sprintId, accountId, departmentId, syllabusId);
        Page<Task> pageData = taskRepository.findAll(spec, pageable);
        return pageData.map(taskMapper::toTaskListResponse);
    }

    public TaskResponse getDetail(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskUpdateRequest request, String accountId) {
        var checkRole = accountService.getAccountById(accountId);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.HOPDC.toString().equals(roleName) ||RoleName.HOCFDC.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        if(!TaskStatus.TO_DO.name().equals(task.getStatus())) {
            throw new AppException(ErrorCode.TASK_NOT_EDITABLE);
        }

        taskMapper.updateTask(task, request);

        // Update Account (mandatory)
        if (request.getAccountId() == null) {
            throw new AppException(ErrorCode.ACCOUNT_ID_REQUIRED);
        }
        if (task.getAccount() == null || !task.getAccount().getAccountId().equals(request.getAccountId())) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
            task.setAccount(account);
        }

        // Update Syllabus if changed
        if (request.getSyllabusId() != null) {
            if (task.getSyllabus() == null || !task.getSyllabus().getSyllabusId().equals(request.getSyllabusId())) {
                Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                        .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
                task.setSyllabus(syllabus);
            }
        } else {
            task.setSyllabus(null);
        }

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public void delete(UUID id, String accountId) {
        var checkRole = accountService.getAccountById(accountId);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.HOPDC.name().equals(roleName) ||RoleName.HOCFDC.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        if(!TaskStatus.TO_DO.name().equals(task.getStatus())) {
            throw new AppException(ErrorCode.TASK_NOT_EDITABLE);
        }

        taskRepository.deleteById(id);
    }

    @Transactional
    public TaskResponse updateStatus(UUID id, String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        task.setStatus(normalizeStatusInput(status, false));
        applyCompletedAtByStatus(task);
        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }


    private String normalizeStatusInput(String rawStatus, boolean useDefaultWhenBlank) {
        if (rawStatus == null || rawStatus.isBlank()) {
            if (useDefaultWhenBlank) {
                return toDisplayStatus(TaskStatus.TO_DO);
            }
            throw new AppException(ErrorCode.INVALID_TASK_STATUS);
        }

        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        if (TaskStatus.TO_DO.name().equals(normalized)) {
            normalized = "TO_DO";
        }
        if (TaskStatus.IN_PROGRESS.name().equals(normalized)) {
            normalized = "IN_PROGRESS";
        }
        if (TaskStatus.DONE.name().equals(normalized)) {
            normalized = "DONE";
        }
        if (TaskStatus.CANCELLED.name().equals(normalized)) {
            normalized = "CANCELLED";
        }
        TaskStatus taskStatus;
        try {
            taskStatus = TaskStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_TASK_STATUS);
        }

        return toDisplayStatus(taskStatus);
    }

    private String toDisplayStatus(TaskStatus status) {
        return switch (status) {
            case DRAFT -> TaskStatus.DRAFT.name();
            case TO_DO -> TaskStatus.TO_DO.name();
            case IN_PROGRESS -> TaskStatus.IN_PROGRESS.name();
            case DONE -> TaskStatus.DONE.name();
            case CANCELLED -> TaskStatus.CANCELLED.name();
        };
    }

    private void applyCompletedAtByStatus(Task task) {
        if (TaskStatus.DONE.name().equalsIgnoreCase(task.getStatus())) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDate.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }
}
