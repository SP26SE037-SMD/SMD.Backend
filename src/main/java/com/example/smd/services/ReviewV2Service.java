package com.example.smd.services;

import com.example.smd.dto.request.reviewV2.ReviewV2CreateRequest;
import com.example.smd.dto.request.reviewV2.ReviewV2UpdateRequest;
import com.example.smd.dto.response.ReviewV2Response;
import com.example.smd.entities.Account;
import com.example.smd.entities.ReviewV2;
import com.example.smd.entities.TaskV2;
import com.example.smd.mapper.ReviewV2Mapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.ReviewV2Repository;
import com.example.smd.repositories.TaskV2Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewV2Service {

    private final ReviewV2Repository reviewV2Repository;
    private final TaskV2Repository    taskV2Repository;
    private final AccountRepository   accountRepository;
    private final ReviewV2Mapper      reviewV2Mapper;

    // ===================== GET ALL (paged + filter) =====================

    @Transactional(readOnly = true)
    public Page<ReviewV2Response> getAll(UUID taskId, Pageable pageable) {
        Page<ReviewV2> page = reviewV2Repository.findAllWithFilters(taskId, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        List<ReviewV2Response> responses = page.getContent().stream()
                .map(reviewV2Mapper::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    // ===================== GET ALL BY TASK =====================

    @Transactional(readOnly = true)
    public List<ReviewV2Response> getAllByTask(UUID taskId) {
        return reviewV2Repository.findAllByTask_TaskId(taskId).stream()
                .map(reviewV2Mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===================== GET BY ID =====================

    @Transactional(readOnly = true)
    public ReviewV2Response getById(UUID reviewId) {
        ReviewV2 review = reviewV2Repository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
        return reviewV2Mapper.toResponse(review);
    }

    // ===================== CREATE =====================

    @Transactional
    public ReviewV2Response create(ReviewV2CreateRequest request) {
        TaskV2 task = taskV2Repository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found: " + request.getTaskId()));

        Account reviewer = accountRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Reviewer (account) not found: " + request.getReviewerId()));

        ReviewV2 review = ReviewV2.builder()
                .task(task)
                .reviewer(reviewer)
                .comment(request.getComment())
                .build();

        return reviewV2Mapper.toResponse(reviewV2Repository.save(review));
    }

    // ===================== UPDATE =====================

    @Transactional
    public ReviewV2Response update(UUID reviewId, ReviewV2UpdateRequest request) {
        ReviewV2 review = reviewV2Repository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        reviewV2Mapper.updateEntity(review, request);

        return reviewV2Mapper.toResponse(reviewV2Repository.save(review));
    }

    // ===================== DELETE =====================

    @Transactional
    public void delete(UUID reviewId) {
        if (!reviewV2Repository.existsById(reviewId)) {
            throw new RuntimeException("Review not found: " + reviewId);
        }
        reviewV2Repository.deleteById(reviewId);
    }
}
