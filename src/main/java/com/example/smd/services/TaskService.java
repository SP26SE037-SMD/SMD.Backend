package com.example.smd.services;

import com.example.smd.dto.request.task.BatchTaskRequest;
import com.example.smd.dto.request.task.BatchTaskItemCreateRequest;
import com.example.smd.dto.request.task.TaskCreateRequest;
import com.example.smd.dto.request.task.TaskUpdateRequest;
import com.example.smd.dto.response.task.TaskListResponse;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Sprint;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Task;
import com.example.smd.enums.RoleName;
import com.example.smd.enums.TaskStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.TaskMapper;
import com.example.smd.repositories.AccountRepository;
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
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
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
    AccountService accountService;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(TaskCreateRequest request, String check) {
        var checkRole = accountService.getAccountById(check);
        String roleName = checkRole.getRole().getRoleName();
        if (!( RoleName.HOCFDC.toString().equals(roleName))) {
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

        task.setStatus(TaskStatus.TO_DO.toString());

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public List<TaskResponse> createBatch(UUID sprintId, BatchTaskRequest request, String check, UUID departmentId) {
        var checkRole = accountService.getAccountById(check);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.HOCFDC.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // Batch API still requires sprintId in path for all roles
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        List<Task> tasksToSave = new ArrayList<>();

        for (BatchTaskItemCreateRequest item : request.getTasks()) {
            Task task = taskMapper.toTask(item);
            task.setSprint(sprint);

            if (item.getSubjectId() != null) {
                Subject subject = subjectRepository.findById(item.getSubjectId())
                        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
                task.setSubject(subject);
            }
            task.setStatus(TaskStatus.TO_DO.toString());

            tasksToSave.add(task);
        }

        List<Task> savedTasks = taskRepository.saveAll(tasksToSave);

        // Send notification to HoPDC accounts in departmentId if provided
        if (departmentId != null) {
            List<Account> hopDCAccounts =
                    accountRepository.findByDepartmentAndRoleName(departmentId, RoleName.HOPDC.toString());
            if (!hopDCAccounts.isEmpty()) {
                // Collect task names for notification
                List<String> taskNames = savedTasks.stream().map(Task::getTaskName).toList();
                // TODO: Implement actual notification logic
                // This could include: sending emails, creating in-app notifications, sending messages, etc.
                // Example: notificationService.notifyHoPDC(hopDCAccounts, taskNames, sprint.getSprintName());
                // For now, log the notification info
                log.info("Notification: Tasks " + taskNames + " created" +
                    " in sprint " + sprint.getSprintName() +
                                   " for HoPDC accounts: " + hopDCAccounts.stream().map(Account::getEmail).toList());
            }
        }

        return savedTasks.stream().map(taskMapper::toTaskResponse).toList();
    }

    public Page<TaskListResponse> getAll(String search, String status, UUID sprintId, UUID accountId, UUID departmentId, UUID syllabusId, Pageable pageable) {
        var spec = TaskSpecification.withFilters(search, status, sprintId, accountId, departmentId, syllabusId);
        Page<Task> pageData = taskRepository.findAll(pageable);
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

        if(!TaskStatus.TO_DO.toString().equals(task.getStatus())) {
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
        if (!(RoleName.HOPDC.toString().equals(roleName) ||RoleName.HOCFDC.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        if(!TaskStatus.TO_DO.toString().equals(task.getStatus())) {
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

    @Transactional(readOnly = true)
    public List<String> getNormalizedStatusOptions() {
        return List.of(
                "To Do (aliases: TODO, TO_DO, To Do)",
                "In Progress (aliases: IN_PROGRESS, In Progress)",
                "In Review (aliases: IN_REVIEW, In Review)",
                "Done (aliases: DONE, Done)",
                "Blocked (aliases: BLOCKED, Blocked)",
                "Cancelled (aliases: CANCELLED, Cancelled)"
        );
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

        if ("TO_DO".equals(normalized)) {
            normalized = "TODO";
        }
        if ("IN_PROGRESS".equals(normalized)) {
            normalized = "IN_PROGRESS";
        }
        if ("IN_REVIEW".equals(normalized)) {
            normalized = "IN_REVIEW";
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
            case DRAFT -> "Draft";
            case TO_DO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Done";
            case CANCELLED -> "Cancelled";
        };
    }

    private void applyCompletedAtByStatus(Task task) {
        if ("Done".equalsIgnoreCase(task.getStatus())) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDate.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }
}
