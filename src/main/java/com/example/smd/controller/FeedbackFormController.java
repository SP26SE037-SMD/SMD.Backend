package com.example.smd.controller;

import com.example.smd.dto.request.feedback.*;
import com.example.smd.dto.response.feedback.*;
import com.example.smd.services.FeedbackFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Feedback Form", description = "Quan ly form feedback va tich hop Google Forms")
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FeedbackFormController {

    private final FeedbackFormService feedbackFormService;

    @PostMapping
//    @PreAuthorize("hasAuthority('FEEDBACK_MANAGE')")
    @Operation(summary = "Tao form feedback moi")
    public ResponseEntity<FormRecordResponse> createForm(@RequestBody CreateFormRequest request) {
        return ResponseEntity.status(201).body(feedbackFormService.createForm(request));
    }

    @GetMapping
//    @PreAuthorize("hasAuthority('FEEDBACK_VIEW')")
    @Operation(summary = "Lay danh sach forms theo curriculum")
    public ResponseEntity<List<FormRecordResponse>> getFormsByCurriculum(@RequestParam UUID curriculumId) {
        return ResponseEntity.ok(feedbackFormService.getFormsByCurriculum(curriculumId));
    }

    @GetMapping("/{formId}")
//    @PreAuthorize("hasAuthority('FEEDBACK_VIEW')")
    @Operation(summary = "Chi tiet form")
    public ResponseEntity<FormDetailResponse> getFormDetail(@PathVariable UUID formId) {
        return ResponseEntity.ok(feedbackFormService.getFormDetail(formId));
    }

    @GetMapping("/{formId}/full")
//    @PreAuthorize("hasAuthority('FEEDBACK_VIEW')")
    @Operation(summary = "Lay toan bo chi tiet form (bao gom section, cau hoi, option) cho Frontend")
    public ResponseEntity<FormSchemaResponse> getFullFormDetail(@PathVariable UUID formId) {
        // Tái sử dụng lại hàm buildFormSchema đã có sẵn trong Service
        return ResponseEntity.ok(feedbackFormService.buildFormSchema(formId));
    }

    @PostMapping("/{formId}/sections")
//    @PreAuthorize("hasAuthority('FEEDBACK_MANAGE')")
    @Operation(summary = "Them section vao form")
    public ResponseEntity<SectionResponse> addSection(@PathVariable UUID formId, @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(201).body(feedbackFormService.addSection(formId, request));
    }

    @PostMapping("/sections/{sectionId}/questions")
//    @PreAuthorize("hasAuthority('FEEDBACK_MANAGE')")
    @Operation(summary = "Them cau hoi vao section")
    public ResponseEntity<QuestionResponse> addQuestion(@PathVariable UUID sectionId, @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(201).body(feedbackFormService.addQuestion(sectionId, request));
    }

    @PostMapping("/{formId}/trigger-build")
//    @PreAuthorize("hasAuthority('FEEDBACK_MANAGE')")
    @Operation(summary = "Kich hoat App Script tao Google Form")
    public ResponseEntity<TriggerBuildResponse> triggerBuildGoogleForm(@PathVariable UUID formId) {
        return ResponseEntity.ok(feedbackFormService.triggerAppScriptBuild(formId));
    }

    @GetMapping("/{formId}/submissions")
//    @PreAuthorize("hasAuthority('FEEDBACK_VIEW')")
    @Operation(summary = "Lay submissions cua form")
    public ResponseEntity<List<FormSubmissionResponse>> getSubmissions(@PathVariable UUID formId) {
        return ResponseEntity.ok(feedbackFormService.getSubmissions(formId));
    }

    @GetMapping("/{formId}/report")
//    @PreAuthorize("hasAuthority('FEEDBACK_VIEW')")
    @Operation(summary = "Bao cao tong hop ket qua feedback")
    public ResponseEntity<FeedbackReportResponse> getReport(@PathVariable UUID formId) {
        return ResponseEntity.ok(feedbackFormService.generateReport(formId));
    }

    @GetMapping("/{formId}/schema")
    @Operation(summary = "Lay form schema cho App Script")
    public ResponseEntity<FormSchemaResponse> getFormSchema(
            @PathVariable UUID formId,
            @RequestHeader("X-Webhook-Secret") String secret) {
        feedbackFormService.validateWebhookSecret(secret);
        return ResponseEntity.ok(feedbackFormService.buildFormSchema(formId));
    }

    @PostMapping("/{formId}/google-form-created")
    @Operation(summary = "Callback tu App Script sau khi tao form")
    public ResponseEntity<Void> onGoogleFormCreated(
            @PathVariable UUID formId,
            @RequestHeader("X-Webhook-Secret") String secret,
            @RequestBody GoogleFormCreatedRequest request) {
        feedbackFormService.validateWebhookSecret(secret);
        feedbackFormService.saveGoogleFormInfo(formId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/submit")
    @Operation(summary = "Webhook nhan du lieu submit tu Google Form")
    public ResponseEntity<WebhookSubmitResponse> receiveFormSubmit(
            @RequestHeader("X-Webhook-Secret") String secret,
            @RequestBody WebhookSubmitRequest request) {
        feedbackFormService.validateWebhookSecret(secret);
        return ResponseEntity.ok(feedbackFormService.processWebhookSubmit(request));
    }
}
