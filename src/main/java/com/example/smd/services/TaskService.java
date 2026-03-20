package com.example.smd.services;

import com.example.smd.dto.request.task.BatchTaskRequest;
import com.example.smd.dto.request.task.TaskItemRequest;
import com.example.smd.dto.request.task.TaskRequest;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.enums.TaskStatus;
import com.example.smd.entities.Sprint;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Task;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.TaskMapper;
import com.example.smd.repositories.AccountRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskService {
    TaskRepository taskRepository;
    SprintRepository sprintRepository;
    AccountRepository accountRepository;
    SyllabusRepository syllabusRepository;
    TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = taskMapper.toTask(request);

        // Map Sprint
        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
        task.setSprint(sprint);

        // Map Account if provided
        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
            task.setAccount(account);
        }

        // Map Syllabus if provided
        if (request.getSyllabusId() != null) {
            Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                    .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
            task.setSyllabus(syllabus);
        }

        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("To Do");
        }

        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }

    @Transactional
    public List<TaskResponse> createBatch(UUID sprintId, BatchTaskRequest request) {
        // Map Sprint
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        List<Task> tasksToSave = new ArrayList<>();

        for (TaskItemRequest item : request.getTasks()) {
            Task task = taskMapper.toTask(item);
            task.setSprint(sprint);

            // Map Account if provided
            if (item.getAccountId() != null) {
                Account account = accountRepository.findById(item.getAccountId())
                        .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
                task.setAccount(account);
            }

            // Map Syllabus if provided
            if (item.getSyllabusId() != null) {
                Syllabus syllabus = syllabusRepository.findById(item.getSyllabusId())
                        .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
                task.setSyllabus(syllabus);
            }

            if (task.getStatus() == null || task.getStatus().isEmpty()) {
                task.setStatus("To Do");
            }

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
            pageData = taskRepository.findByStatus(status, pageable);
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

        // Update Sprint if changed
        if (request.getSprintId() != null && (task.getSprint() == null || !task.getSprint().getSprintId().equals(request.getSprintId()))) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
            task.setSprint(sprint);
        }

        // Update Account if changed
        if (request.getAccountId() != null) {
            if (task.getAccount() == null || !task.getAccount().getAccountId().equals(request.getAccountId())) {
                Account account = accountRepository.findById(request.getAccountId())
                        .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
                task.setAccount(account);
            }
        } else {
            task.setAccount(null);
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
    public void delete(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new AppException(ErrorCode.TASK_NOT_FOUND);
        }
        taskRepository.deleteById(id);
    }

    @Transactional
    public TaskResponse updateStatus(UUID id, String status) {
        boolean isValid = java.util.Arrays.stream(TaskStatus.values())
                .anyMatch(s -> s.name().equalsIgnoreCase(status));
        if (!isValid) {
            throw new AppException(ErrorCode.INVALID_TASK_STATUS);
        }

        TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        task.setStatus(taskStatus.name());
        task = taskRepository.save(task);
        return taskMapper.toTaskResponse(task);
    }
}
