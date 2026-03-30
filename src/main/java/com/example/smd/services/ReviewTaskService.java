package com.example.smd.services;

import com.example.smd.dto.request.reviewtask.ReviewTaskRequest;
import com.example.smd.dto.response.reviewtask.ReviewTaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.ReviewTask;
import com.example.smd.entities.Task;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ReviewTaskMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.ReviewTaskRepository;
import com.example.smd.repositories.TaskRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewTaskService {

    ReviewTaskRepository reviewTaskRepository;
    TaskRepository taskRepository;
    AccountRepository accountRepository;
    ReviewTaskMapper reviewTaskMapper;

    @Transactional
    public ReviewTaskResponse create(ReviewTaskRequest request, String reviewerAccountId) {
        validateRequest(request);

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        UUID reviewerId = request.getReviewerId();
        if (reviewerId == null && reviewerAccountId != null && !reviewerAccountId.isBlank()) {
            reviewerId = UUID.fromString(reviewerAccountId);
        }
        if (reviewerId == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Reviewer ID is required");
        }

        Account reviewer = accountRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        ReviewTask reviewTask = reviewTaskMapper.toReviewTask(request);
        reviewTask.setTask(task);
        reviewTask.setReviewer(reviewer);

        if (reviewTask.getReviewDate() == null) {
            reviewTask.setReviewDate(Instant.now());
        }
        if (reviewTask.getStatus() == null || reviewTask.getStatus().isBlank()) {
            reviewTask.setStatus("Pending");
        }

        reviewTask = reviewTaskRepository.save(reviewTask);
        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    public Page<ReviewTaskResponse> getAll(String search, String status, UUID taskId, UUID reviewerId, Pageable pageable) {
        Page<ReviewTask> pageData;

        if (search != null && !search.isBlank()) {
            pageData = reviewTaskRepository.findByTitleTaskContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isBlank()) {
            pageData = reviewTaskRepository.findByStatusIgnoreCase(status, pageable);
        } else if (taskId != null) {
            pageData = reviewTaskRepository.findByTask_TaskId(taskId, pageable);
        } else if (reviewerId != null) {
            pageData = reviewTaskRepository.findByReviewer_AccountId(reviewerId, pageable);
        } else {
            pageData = reviewTaskRepository.findAll(pageable);
        }

        return pageData.map(reviewTaskMapper::toReviewTaskResponse);
    }

    public ReviewTaskResponse getDetail(UUID id) {
        ReviewTask reviewTask = reviewTaskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND));

        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    @Transactional
    public ReviewTaskResponse update(UUID id, ReviewTaskRequest request) {
        validateRequest(request);

        ReviewTask reviewTask = reviewTaskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND));

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        reviewTaskMapper.updateReviewTask(reviewTask, request);
        reviewTask.setTask(task);

        if (reviewTask.getReviewDate() == null) {
            reviewTask.setReviewDate(Instant.now());
        }

        reviewTask = reviewTaskRepository.save(reviewTask);
        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    @Transactional
    public void delete(UUID id) {
        if (!reviewTaskRepository.existsById(id)) {
            throw new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND);
        }

        reviewTaskRepository.deleteById(id);
    }

    @Transactional
    public ReviewTaskResponse updateStatus(UUID id, String status) {
        if (status == null || status.isBlank()) {
            throw new AppException(ErrorCode.REVIEW_TASK_STATUS_REQUIRED);
        }

        ReviewTask reviewTask = reviewTaskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND));

        reviewTask.setStatus(status.trim());
        reviewTask = reviewTaskRepository.save(reviewTask);

        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    private void validateRequest(ReviewTaskRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.REVIEW_TASK_REQUEST_REQUIRED);
        }

        if (request.getTaskId() == null) {
            throw new AppException(ErrorCode.REVIEW_TASK_TASK_ID_REQUIRED);
        }
    }
}
