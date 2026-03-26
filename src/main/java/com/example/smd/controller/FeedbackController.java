package com.example.smd.controller;

import com.example.smd.dto.request.feedback.*;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.feedback.*;
import com.example.smd.services.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Feedback", description = "Feedback form management and user opinion collection APIs")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    FeedbackService feedbackService;

    @PostMapping("/questions")
    @Operation(summary = "Create one feedback question")
    public ResponseObject<FeedbackQuestionResponse> createQuestion(@RequestBody FeedbackQuestionRequest request) {
        return ResponseObject.<FeedbackQuestionResponse>builder()
                .status(1000)
                .data(feedbackService.createQuestion(request))
                .message("Feedback question created successfully")
                .build();
    }

    @PostMapping("/questions/batch")
    @Operation(summary = "Create multiple feedback questions")
    public ResponseObject<List<FeedbackQuestionResponse>> createQuestionsBatch(@RequestBody FeedbackQuestionBatchRequest request) {
        return ResponseObject.<List<FeedbackQuestionResponse>>builder()
                .status(1000)
                .data(feedbackService.createQuestionsBatch(request))
                .message("Feedback questions created successfully")
                .build();
    }

    @GetMapping("/questions")
    @Operation(summary = "Get all feedback questions", description = "Optional formType filter")
    public ResponseObject<List<FeedbackQuestionResponse>> getAllQuestions(@RequestParam(required = false) String formType) {
        return ResponseObject.<List<FeedbackQuestionResponse>>builder()
                .status(1000)
                .data(feedbackService.getAllQuestions(formType))
                .message("Feedback questions retrieved successfully")
                .build();
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Get feedback question detail")
    public ResponseObject<FeedbackQuestionResponse> getQuestionDetail(@PathVariable UUID questionId) {
        return ResponseObject.<FeedbackQuestionResponse>builder()
                .status(1000)
                .data(feedbackService.getQuestionDetail(questionId))
                .message("Feedback question detail retrieved successfully")
                .build();
    }

    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Update feedback question")
    public ResponseObject<FeedbackQuestionResponse> updateQuestion(
            @PathVariable UUID questionId,
            @RequestBody FeedbackQuestionRequest request) {
        return ResponseObject.<FeedbackQuestionResponse>builder()
                .status(1000)
                .data(feedbackService.updateQuestion(questionId, request))
                .message("Feedback question updated successfully")
                .build();
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete feedback question")
    public ResponseObject<Void> deleteQuestion(@PathVariable UUID questionId) {
        feedbackService.deleteQuestion(questionId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Feedback question deleted successfully")
                .build();
    }

    @PostMapping("/options")
    @Operation(summary = "Create one feedback option")
    public ResponseObject<FeedbackOptionResponse> createOption(@RequestBody FeedbackOptionRequest request) {
        return ResponseObject.<FeedbackOptionResponse>builder()
                .status(1000)
                .data(feedbackService.createOption(request))
                .message("Feedback option created successfully")
                .build();
    }

    @PostMapping("/options/batch")
    @Operation(summary = "Create multiple feedback options")
    public ResponseObject<List<FeedbackOptionResponse>> createOptionsBatch(@RequestBody FeedbackOptionBatchRequest request) {
        return ResponseObject.<List<FeedbackOptionResponse>>builder()
                .status(1000)
                .data(feedbackService.createOptionsBatch(request))
                .message("Feedback options created successfully")
                .build();
    }

    @GetMapping("/options")
    @Operation(summary = "Get all feedback options")
    public ResponseObject<List<FeedbackOptionResponse>> getAllOptions() {
        return ResponseObject.<List<FeedbackOptionResponse>>builder()
                .status(1000)
                .data(feedbackService.getAllOptions())
                .message("Feedback options retrieved successfully")
                .build();
    }

    @GetMapping("/options/{optionId}")
    @Operation(summary = "Get feedback option detail")
    public ResponseObject<FeedbackOptionResponse> getOptionDetail(@PathVariable UUID optionId) {
        return ResponseObject.<FeedbackOptionResponse>builder()
                .status(1000)
                .data(feedbackService.getOptionDetail(optionId))
                .message("Feedback option detail retrieved successfully")
                .build();
    }

    @PutMapping("/options/{optionId}")
    @Operation(summary = "Update feedback option")
    public ResponseObject<FeedbackOptionResponse> updateOption(
            @PathVariable UUID optionId,
            @RequestBody FeedbackOptionRequest request) {
        return ResponseObject.<FeedbackOptionResponse>builder()
                .status(1000)
                .data(feedbackService.updateOption(optionId, request))
                .message("Feedback option updated successfully")
                .build();
    }

    @DeleteMapping("/options/{optionId}")
    @Operation(summary = "Delete feedback option")
    public ResponseObject<Void> deleteOption(@PathVariable UUID optionId) {
        feedbackService.deleteOption(optionId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Feedback option deleted successfully")
                .build();
    }

    @GetMapping("/forms/{formType}")
    @Operation(summary = "Get feedback form by formType", description = "Returns question list and option list for rendering form")
    public ResponseObject<FeedbackFormResponse> getForm(@PathVariable String formType) {
        return ResponseObject.<FeedbackFormResponse>builder()
                .status(1000)
                .data(feedbackService.getForm(formType))
                .message("Feedback form retrieved successfully")
                .build();
    }

    @PostMapping("/submissions")
    @Operation(summary = "Submit one feedback submission with answers")
    public ResponseObject<FeedbackSubmissionResponse> submitFeedback(@RequestBody FeedbackSubmissionRequest request) {
        return ResponseObject.<FeedbackSubmissionResponse>builder()
                .status(1000)
                .data(feedbackService.createSubmission(request))
                .message("Feedback submitted successfully")
                .build();
    }

    @PostMapping("/submissions/batch")
    @Operation(summary = "Submit multiple feedback submissions")
    public ResponseObject<List<FeedbackSubmissionResponse>> submitFeedbackBatch(@RequestBody FeedbackSubmissionBatchRequest request) {
        return ResponseObject.<List<FeedbackSubmissionResponse>>builder()
                .status(1000)
                .data(feedbackService.createSubmissionBatch(request))
                .message("Feedback submissions created successfully")
                .build();
    }

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "Get feedback submission detail")
    public ResponseObject<FeedbackSubmissionResponse> getSubmissionDetail(@PathVariable UUID submissionId) {
        return ResponseObject.<FeedbackSubmissionResponse>builder()
                .status(1000)
                .data(feedbackService.getSubmissionDetail(submissionId))
                .message("Feedback submission detail retrieved successfully")
                .build();
    }

    @GetMapping("/submissions/curriculum/{curriculumId}")
    @Operation(summary = "Get all feedback submissions by curriculum")
    public ResponseObject<List<FeedbackSubmissionResponse>> getSubmissionsByCurriculum(@PathVariable UUID curriculumId) {
        return ResponseObject.<List<FeedbackSubmissionResponse>>builder()
                .status(1000)
                .data(feedbackService.getSubmissionsByCurriculum(curriculumId))
                .message("Feedback submissions retrieved successfully")
                .build();
    }

    @GetMapping("/submissions/account/{accountId}")
    @Operation(summary = "Get all feedback submissions by account")
    public ResponseObject<List<FeedbackSubmissionResponse>> getSubmissionsByAccount(@PathVariable UUID accountId) {
        return ResponseObject.<List<FeedbackSubmissionResponse>>builder()
                .status(1000)
                .data(feedbackService.getSubmissionsByAccount(accountId))
                .message("Feedback submissions by account retrieved successfully")
                .build();
    }

    @GetMapping("/summary/curriculum/{curriculumId}")
    @Operation(summary = "Get aggregated feedback summary by curriculum")
    public ResponseObject<FeedbackSummaryResponse> getSummaryByCurriculum(@PathVariable UUID curriculumId) {
        return ResponseObject.<FeedbackSummaryResponse>builder()
                .status(1000)
                .data(feedbackService.getSummaryByCurriculum(curriculumId))
                .message("Feedback summary retrieved successfully")
                .build();
    }
}
