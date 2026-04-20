package com.example.smd.services;

import com.example.smd.dto.request.reviewtask.ReviewTaskAcceptanceRequest;
import com.example.smd.dto.request.reviewtask.ReviewTaskCreateHoCFDC;
import com.example.smd.dto.request.reviewtask.ReviewTaskCreateRequest;
import com.example.smd.dto.request.reviewtask.ReviewTaskRequest;
import com.example.smd.dto.response.reviewtask.ReviewTaskResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.ReviewTask;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Task;
import com.example.smd.enums.ReviewStatus;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.enums.TaskStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ReviewTaskMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.AssessmentRepository;
import com.example.smd.repositories.MaterialRepository;
import com.example.smd.repositories.ReviewTaskRepository;
import com.example.smd.repositories.ReviewTaskSpecification;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
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
    SyllabusRepository syllabusRepository;
    MaterialRepository materialRepository;
    SessionRepository sessionRepository;
    AssessmentRepository assessmentRepository;
    SubjectRepository subjectRepository;

    @Transactional
    public ReviewTaskResponse create(ReviewTaskCreateRequest request,
            String reviewerAccountId) {

        var check = reviewTaskMapper.toReviewTaskRequest(request);
        validateRequest(check);

        Task task = taskRepository.findById(check.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        UUID reviewerId = check.getReviewerId();
        if (reviewerId == null && reviewerAccountId != null && !reviewerAccountId.isBlank()) {
            reviewerId = UUID.fromString(reviewerAccountId);
        }
        if (reviewerId == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Reviewer ID is required");
        }

        Account reviewer = accountRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        ReviewTask reviewTask = reviewTaskMapper.toReviewTask(check);
        reviewTask.setTask(task);
        reviewTask.setReviewer(reviewer);

        if (reviewTask.getDueDate() == null) {
            reviewTask.setDueDate(Instant.now());
        }
        if (reviewTask.getStatus() == null || reviewTask.getStatus().isBlank()) {
            reviewTask.setStatus(ReviewStatus.PENDING.name());
        }

        reviewTask = reviewTaskRepository.save(reviewTask);

        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    private void applyHoCFDCCascadeStatuses(Task task, Boolean isAccepted) {
        if (task == null) {
            return;
        }

        Syllabus syllabus = task.getSyllabus();

        if (Boolean.TRUE.equals(isAccepted)) {
            if (syllabus != null && syllabus.getSubject() != null) {
                syllabus.getSubject().setStatus(SubjectStatus.COMPLETED.name());
                subjectRepository.save(syllabus.getSubject());
            }
            return;
        }

        task.setStatus(TaskStatus.IN_PROGRESS.name());
        taskRepository.save(task);

        if (syllabus == null) {
            return;
        }

        if (syllabus.getSubject() != null) {
            syllabus.getSubject().setStatus(SubjectStatus.WAITING_SYLLABUS.name());
            subjectRepository.save(syllabus.getSubject());
        }

        syllabus.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
        syllabusRepository.save(syllabus);

        materialRepository.findBySyllabus_SyllabusId(syllabus.getSyllabusId())
                .forEach(m -> {
                    m.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
                    materialRepository.save(m);
                });

        sessionRepository.findBySyllabus_SyllabusId(syllabus.getSyllabusId())
                .forEach(s -> {
                    s.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
                    sessionRepository.save(s);
                });

        assessmentRepository.findBySyllabus_SyllabusId(syllabus.getSyllabusId())
                .forEach(a -> {
                    a.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
                    assessmentRepository.save(a);
                });
    }

    @Transactional
    public ReviewTaskResponse createByHoCFDC(ReviewTaskCreateHoCFDC request,
            String reviewerAccountId) {

        var check = reviewTaskMapper.toReviewTaskRequestHoCFDC(request);
        validateRequest(check);

        Task task = taskRepository.findById(check.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        UUID reviewerId = check.getReviewerId();
        if (reviewerId == null && reviewerAccountId != null && !reviewerAccountId.isBlank()) {
            reviewerId = UUID.fromString(reviewerAccountId);
        }
        if (reviewerId == null) {
            throw new AppException(ErrorCode.INVALID_KEY, "Reviewer ID is required");
        }

        Account reviewer = accountRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        ReviewTask reviewTask = reviewTaskMapper.toReviewTask(check);
        reviewTask.setTask(task);
        reviewTask.setReviewer(reviewer);
        reviewTask.setIsAccepted(request.getIsAccepted());

        if (reviewTask.getReviewDate() == null) {
            reviewTask.setReviewDate(Instant.now());
        }
        if (reviewTask.getStatus() == null || reviewTask.getStatus().isBlank()) {
            reviewTask.setStatus(ReviewStatus.APPROVED.name());
        }
        if (reviewTask.getIsAccepted() == null) {
            reviewTask.setIsAccepted(Boolean.FALSE);
        }

        reviewTask = reviewTaskRepository.save(reviewTask);

        applyHoCFDCCascadeStatuses(reviewTask.getTask(), reviewTask.getIsAccepted());

        return reviewTaskMapper.toReviewTaskResponse(reviewTask);
    }

    public Page<ReviewTaskResponse> getAll(String search, String status, UUID taskId, UUID reviewerId,
            Pageable pageable) {
        var spec = ReviewTaskSpecification.withFilters(search, status, taskId, reviewerId);
        Page<ReviewTask> pageData = reviewTaskRepository.findAll(spec, pageable);

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

    @Transactional
    public ReviewTaskResponse updateAcceptance(UUID id,
                                               ReviewTaskAcceptanceRequest request) {
        ReviewTask reviewTask = reviewTaskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND));
        reviewTask.setIsAccepted(request.getIsAccepted());
        if(request.getComment() != null) {
            reviewTask.setComment(request.getComment());
        }
        reviewTask = reviewTaskRepository.save(reviewTask);

        // Trigger cascading updates based on acceptance status and current status
        Task task = reviewTask.getTask();
        if (task != null && task.getSyllabus() != null) {
            Syllabus syllabus = task.getSyllabus();
            String reviewTaskStatus = reviewTask.getStatus();

            if (request.getIsAccepted() && ReviewStatus.REVISION_REQUESTED.name().equals(reviewTaskStatus)) {
                //True
                // Syllabus → REVISION_REQUESTED
                syllabus.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
                syllabusRepository.save(syllabus);

            } else if (request.getIsAccepted() && ReviewStatus.APPROVED.name().equals(reviewTaskStatus)) {
                //True
                // Syllabus → APPROVED
                // Subject → PENDING_REVIEW
                // Task → DONE
                syllabus.setStatus(SyllabusStatus.APPROVED.name());
                syllabusRepository.save(syllabus);

                if (syllabus.getSubject() != null) {
                    syllabus.getSubject().setStatus(SubjectStatus.PENDING_REVIEW.name());
                    subjectRepository.save(syllabus.getSubject());
                }

                task.setStatus(TaskStatus.DONE.name());
                taskRepository.save(task);
            } else if (!request.getIsAccepted() && ReviewStatus.REVISION_REQUESTED.name().equals(reviewTaskStatus)) {
                //False
                //Review Task → APPROVED
                // Syllabus → APPROVED
                // Subject → PENDING_REVIEW
                // Task → DONE

                reviewTask.setStatus(ReviewStatus.APPROVED.name());
                reviewTaskRepository.save(reviewTask);

                syllabus.setStatus(SyllabusStatus.APPROVED.name());
                syllabusRepository.save(syllabus);


                if (syllabus.getSubject() != null) {
                    syllabus.getSubject().setStatus(SubjectStatus.PENDING_REVIEW.name());
                    subjectRepository.save(syllabus.getSubject());
                }

                task.setStatus(TaskStatus.DONE.name());
                taskRepository.save(task);
            } else if (!request.getIsAccepted() && ReviewStatus.APPROVED.name().equals(reviewTaskStatus)) {
                //False
                //Review Task → REVISION_REQUESTED
                // Syllabus → REVISION_REQUESTED

                reviewTask.setStatus(ReviewStatus.REVISION_REQUESTED.name());
                reviewTaskRepository.save(reviewTask);

                syllabus.setStatus(SyllabusStatus.REVISION_REQUESTED.name());
                syllabusRepository.save(syllabus);

            }
        }

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
