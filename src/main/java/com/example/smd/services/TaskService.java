package com.example.smd.services;

import com.example.smd.dto.request.task.BatchTaskRequest;
import com.example.smd.dto.request.task.TaskItemRequest;
import com.example.smd.dto.request.task.TaskRequest;
import com.example.smd.dto.response.task.TaskCurriculumResponse;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Sprint;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Task;
import com.example.smd.enums.TaskStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.TaskMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.SprintRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.repositories.TaskRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskService {
    TaskRepository taskRepository;
    SprintRepository sprintRepository;
    AccountRepository accountRepository;
    SyllabusRepository syllabusRepository;
    CurriculumRepository curriculumRepository;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = taskMapper.toTask(request);

        // sprintId is mandatory for all users
        if(request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
            task.setSprint(sprint);
        }


        // accountId is mandatory
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        task.setAccount(account);

        // Map Syllabus if provided
        if (request.getSyllabusId() != null) {
            Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                    .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
            task.setSyllabus(syllabus);
        }

        // Map Curriculum if provided
        if (request.getCurriculumId() != null) {
            Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                    .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
            task.setCurriculum(curriculum);
        }

        if (request.getCreatedAt() != null) {
            task.setCreatedAt(request.getCreatedAt());
        }

        task.setStatus(normalizeStatusInput(task.getStatus(), true));
        applyCompletedAtByStatus(task);

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public List<TaskResponse> createBatch(UUID sprintId, BatchTaskRequest request) {
        // Batch API still requires sprintId in path for all roles
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        List<Task> tasksToSave = new ArrayList<>();

        for (TaskItemRequest item : request.getTasks()) {
            Task task = taskMapper.toTask(item);
            task.setSprint(sprint);

                // accountId is mandatory
                Account account = accountRepository.findById(item.getAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
                task.setAccount(account);

            // Map Syllabus if provided
            if (item.getSyllabusId() != null) {
                Syllabus syllabus = syllabusRepository.findById(item.getSyllabusId())
                        .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
                task.setSyllabus(syllabus);
            }

            if (item.getCurriculumId() != null) {
                Curriculum curriculum = curriculumRepository.findById(item.getCurriculumId())
                        .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
                task.setCurriculum(curriculum);
            }

            if (item.getCreatedAt() != null) {
                task.setCreatedAt(item.getCreatedAt());
            }

            task.setStatus(normalizeStatusInput(task.getStatus(), true));
            applyCompletedAtByStatus(task);

            tasksToSave.add(task);
        }

        List<Task> savedTasks = taskRepository.saveAll(tasksToSave);
        return savedTasks.stream().map(taskMapper::toTaskResponse).toList();
    }

    public Page<TaskResponse> getAll(String search, String status, UUID sprintId, UUID accountId, Pageable pageable) {
        Page<Task> pageData;
        if (search != null && !search.isEmpty()) {
            pageData = taskRepository.findByTaskNameContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            pageData = taskRepository.findByStatusIgnoreCase(normalizeStatusInput(status, false), pageable);
        } else if (sprintId != null) {
            pageData = taskRepository.findBySprint_SprintId(sprintId, pageable);
        } else if (accountId != null) {
            pageData = taskRepository.findByAccount_AccountId(accountId, pageable);
        } else {
            pageData = taskRepository.findAll(pageable);
        }

        return pageData.map(taskMapper::toTaskResponse);
    }

    public TaskResponse getDetail(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        taskMapper.updateTask(task, request);

        // Update Sprint (mandatory for all users)
        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
        task.setSprint(sprint);

        // Update Account (mandatory)
        if (request.getAccountId() == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
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

        // Update Curriculum if changed
        if (request.getCurriculumId() != null) {
            if (task.getCurriculum() == null ||
                !task.getCurriculum().getCurriculumId().equals(request.getCurriculumId())) {
                Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                        .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
                task.setCurriculum(curriculum);
            }
        } else {
            task.setCurriculum(null);
        }

        if (request.getCreatedAt() != null) {
            task.setCreatedAt(request.getCreatedAt());
        }

        task.setStatus(normalizeStatusInput(task.getStatus(), true));
        applyCompletedAtByStatus(task);

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public void delete(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new AppException(ErrorCode.TASK_NOT_FOUND);
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
    public List<TaskCurriculumResponse> getCurriculumIdsByAccountId(UUID accountId, String curriculumStatus) {
        if (!accountRepository.existsById(accountId)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        String normalizedStatus = curriculumStatus == null ? null : curriculumStatus.trim();
        if (normalizedStatus != null && normalizedStatus.isEmpty()) {
            normalizedStatus = null;
        }

        List<Curriculum> curriculums =
                taskRepository.findDistinctCurriculumsByAccountIdAndStatus(accountId, normalizedStatus);

        return curriculums.stream()
                .map(curriculum -> TaskCurriculumResponse.builder()
                        .curriculumId(curriculum.getCurriculumId())
                        .curriculumCode(curriculum.getCurriculumCode())
                        .curriculumName(curriculum.getCurriculumName())
                        .startYear(curriculum.getStartYear())
                        .status(curriculum.getStatus())
                        .build())
                .toList();
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
                return toDisplayStatus(TaskStatus.TODO);
            }
            throw new AppException(ErrorCode.INVALID_TASK_STATUS);
        }

        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        if ("TO_DO".equals(normalized)) {
            normalized = "TODO";
        }
        if ("INPROGRESS".equals(normalized)) {
            normalized = "IN_PROGRESS";
        }
        if ("INREVIEW".equals(normalized)) {
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
            case TODO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case IN_REVIEW -> "In Review";
            case DONE -> "Done";
            case BLOCKED -> "Blocked";
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
