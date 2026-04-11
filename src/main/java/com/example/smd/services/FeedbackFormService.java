package com.example.smd.services;

import com.example.smd.dto.request.feedback.*;
import com.example.smd.dto.response.feedback.*;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackFormService {

    final GoogleFormRecordRepository formRecordRepo;
    final FormQuestionMappingRepository questionMappingRepo;
    final FeedbackFormSectionRepository sectionRepo;
    final FeedbackFormQuestionRepository questionRepo;
    final FeedbackFormOptionRepository optionRepo;
    final FeedbackSubmissionRepository submissionRepo;
    final FeedbackAnswerRepository answerRepo;
    final CurriculumRepository curriculumRepo;
    final AccountRepository accountRepository;

    final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.webhook.secret}")
    String webhookSecret;

    @Value("${app.appscript.deploy-url}")
    String appScriptDeployUrl;

    public void validateWebhookSecret(String incoming) {
        if (incoming == null || !webhookSecret.equals(incoming)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Transactional
    public FormRecordResponse createForm(CreateFormRequest req) {
        if (req == null || req.getCurriculumId() == null) {
            throw new AppException(ErrorCode.FEEDBACK_CURRICULUM_ID_REQUIRED);
        }
        if (req.getFormType() == null || req.getFormType().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_FORM_TYPE_REQUIRED);
        }

        Curriculum curriculum = curriculumRepo.findById(req.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        GoogleFormRecord record = GoogleFormRecord.builder()
                .curriculum(curriculum)
                .formType(req.getFormType().trim())
                .isActive(false)
                .build();

        record = formRecordRepo.save(record);
        return toFormRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public List<FormRecordResponse> getFormsByCurriculum(UUID curriculumId) {
        return formRecordRepo.findByCurriculum_CurriculumId(curriculumId)
                .stream()
                .map(this::toFormRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FormDetailResponse getFormDetail(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);
        List<FeedbackFormSection> sections = sectionRepo.findByFormRecord_IdOrderByOrderIndexAsc(formId);

        return FormDetailResponse.builder()
                .id(record.getId().toString())
                .googleFormId(record.getGoogleFormId())
                .formUrl(record.getFormUrl())
                .isActive(record.getIsActive())
                .sections(sections.stream().map(this::toSectionResponse).toList())
                .build();
    }

    @Transactional
    public SectionResponse addSection(UUID formId, CreateSectionRequest req) {
        GoogleFormRecord record = findFormRecord(formId);
        int maxOrder = sectionRepo.findMaxOrderIndex(formId).orElse(0);

        FeedbackFormSection section = FeedbackFormSection.builder()
                .formRecord(record)
                .title(req != null ? req.getTitle() : null)
                .orderIndex(maxOrder + 1)
                .afterSectionAction(req != null && req.getAfterSectionAction() != null
                        ? req.getAfterSectionAction().trim()
                        : "NEXT")
                .targetSectionId(req != null ? req.getTargetSectionId() : null)
                .build();

        section = sectionRepo.save(section);
        return toSectionResponse(section);
    }

    @Transactional
    public QuestionResponse addQuestion(UUID sectionId, CreateQuestionRequest req) {
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_TEXT_REQUIRED);
        }

        FeedbackFormSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_SECTION_NOT_FOUND));

        int maxOrder = questionRepo.findMaxOrderIndex(sectionId).orElse(0);

        FeedbackFormQuestion question = FeedbackFormQuestion.builder()
                .section(section)
                .content(req.getContent().trim())
                .type(req.getType())
                .isRequired(Boolean.TRUE.equals(req.getIsRequired()))
                .orderIndex(maxOrder + 1)
                .build();
        question = questionRepo.save(question);

        if (req.getOptions() != null && !req.getOptions().isEmpty()) {
            int optionNo = 1;
            for (CreateOptionRequest optReq : req.getOptions()) {
                FeedbackFormOption formOption = FeedbackFormOption.builder()
                        .question(question)
                        .optionText(optReq.getOptionText())
                        .orderIndex(optionNo)
                        .nextSectionId(optReq.getNextSectionId())
                        .build();
                optionRepo.save(formOption);
                optionNo++;
            }
        }

        return toQuestionResponse(question);
    }

    @Transactional
    public FormRecordResponse updateForm(UUID formId, UpdateFormRequest req) {
        GoogleFormRecord record = findFormRecord(formId);
        if (req != null) {
            if (req.getFormType() != null && !req.getFormType().isBlank()) {
                record.setFormType(req.getFormType().trim());
            }
        }
        formRecordRepo.save(record);
        return toFormRecordResponse(record);
    }

    @Transactional
    public void deleteForm(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);
        formRecordRepo.delete(record);
    }
    
    @Transactional
    public SectionResponse updateSection(UUID sectionId, UpdateSectionRequest req) {
        FeedbackFormSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_SECTION_NOT_FOUND));
                
        if (req != null) {
            if (req.getTitle() != null) section.setTitle(req.getTitle());
            if (req.getOrderIndex() != null) section.setOrderIndex(req.getOrderIndex());
            if (req.getAfterSectionAction() != null) section.setAfterSectionAction(req.getAfterSectionAction());
            section.setTargetSectionId(req.getTargetSectionId());
        }
        sectionRepo.save(section);
        return toSectionResponse(section);
    }
    
    @Transactional
    public void deleteSection(UUID sectionId) {
        FeedbackFormSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_SECTION_NOT_FOUND));
        sectionRepo.delete(section);
    }
    
    @Transactional
    public QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest req) {
        FeedbackFormQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));
                
        if (req != null) {
            if (req.getContent() != null && !req.getContent().isBlank()) {
                question.setContent(req.getContent().trim());
            }
            if (req.getType() != null) question.setType(req.getType());
            if (req.getIsRequired() != null) question.setIsRequired(req.getIsRequired());
            if (req.getOrderIndex() != null) question.setOrderIndex(req.getOrderIndex());
            
            // Xóa option cũ
            optionRepo.deleteAll(optionRepo.findByQuestion_QuestionIdOrderByOrderIndexAsc(questionId));
            if (question.getOptions() != null) {
                question.getOptions().clear();
            }
            
            // Chèn option mới
            if (req.getOptions() != null && !req.getOptions().isEmpty()) {
                int optionNo = 1;
                for (CreateOptionRequest optReq : req.getOptions()) {
                    FeedbackFormOption formOption = FeedbackFormOption.builder()
                            .question(question)
                            .optionText(optReq.getOptionText())
                            .orderIndex(optionNo)
                            .nextSectionId(optReq.getNextSectionId())
                            .build();
                    optionRepo.save(formOption);
                    if (question.getOptions() != null) {
                        question.getOptions().add(formOption);
                    }
                    optionNo++;
                }
            }
        }
        questionRepo.save(question);
        return toQuestionResponse(question);
    }
    
    @Transactional
    public void deleteQuestion(UUID questionId) {
        FeedbackFormQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));
        questionRepo.delete(question);
    }

    @Transactional(readOnly = true)
    public FormSchemaResponse buildFormSchema(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);
        List<FeedbackFormSection> sections = sectionRepo.findByFormRecord_IdOrderByOrderIndexAsc(formId);

        List<FormSchemaResponse.SectionSchema> sectionSchemas = sections.stream().map(sec -> {
            List<FeedbackFormQuestion> questions = questionRepo.findBySection_SectionIdOrderByOrderIndexAsc(sec.getSectionId());

            List<FormSchemaResponse.QuestionSchema> questionSchemas = questions.stream().map(q -> {
                List<FeedbackFormOption> options = optionRepo.findByQuestion_QuestionIdOrderByOrderIndexAsc(q.getQuestionId());

                List<FormSchemaResponse.OptionSchema> optionSchemas = options.stream().map(o ->
                        FormSchemaResponse.OptionSchema.builder()
                                .optionId(o.getOptionId().toString())
                                .text(o.getOptionText())
                                .goToSectionId(o.getNextSectionId() != null ? o.getNextSectionId().toString() : null)
                                .build()
                ).toList();

                return FormSchemaResponse.QuestionSchema.builder()
                        .questionId(q.getQuestionId().toString())
                        .type(q.getType())
                        .content(q.getContent())
                        .isRequired(q.getIsRequired())
                        .options(optionSchemas)
                        .build();
            }).toList();

            return FormSchemaResponse.SectionSchema.builder()
                    .sectionId(sec.getSectionId().toString())
                    .title(sec.getTitle())
                    .actionAfter(sec.getAfterSectionAction())
                    .targetSectionId(sec.getTargetSectionId() != null ? sec.getTargetSectionId().toString() : null)
                    .questions(questionSchemas)
                    .build();
        }).toList();

        return FormSchemaResponse.builder()
                .formId(formId.toString())
                .title(buildFormTitle(record))
                .description("Phan hoi cho chuong trinh dao tao: " + record.getCurriculum().getCurriculumName())
                .sections(sectionSchemas)
                .build();
    }

    @Transactional
    public TriggerBuildResponse triggerAppScriptBuild(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);

        Map<String, Object> body = Map.of(
                "action", "buildForm",
                "formId", formId.toString(),
                "secret", webhookSecret,
                "oldGoogleFormId", record.getGoogleFormId() != null ? record.getGoogleFormId().trim() : null
        );

//        // Nếu đã có form cũ, gửi ID để App Script xóa trên Google Drive
//        if (record.getGoogleFormId() != null && !record.getGoogleFormId().isBlank()) {
//            body.put("oldGoogleFormId", record.getGoogleFormId().trim());
//        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.ALL));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(appScriptDeployUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is3xxRedirection() && response.getHeaders().getLocation() != null) {
                URI redirectUri = response.getHeaders().getLocation();
                ResponseEntity<String> redirectedResponse = restTemplate.exchange(redirectUri, HttpMethod.GET, HttpEntity.EMPTY, String.class);
                log.info("App Script redirect status={}, redirectUri={}, finalStatus={}, finalBody={}",
                        response.getStatusCode(), redirectUri, redirectedResponse.getStatusCode(), redirectedResponse.getBody());
            } else {
                log.info("App Script build response status={}, body={}", response.getStatusCode(), response.getBody());
            }

            return TriggerBuildResponse.builder()
                    .success(true)
                    .message("App Script dang xay dung Google Form")
                    .build();
        } catch (Exception e) {
            log.error("Trigger App Script failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.APP_SCRIPT_CALL_FAILED);
        }
    }

    @Transactional
    public void saveGoogleFormInfo(UUID formId, GoogleFormCreatedRequest req) {
        if (req == null || req.getGoogleFormId() == null || req.getGoogleFormId().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_FORM_NOT_FOUND);
        }

        GoogleFormRecord record = findFormRecord(formId);
        record.setGoogleFormId(req.getGoogleFormId().trim());
        record.setFormUrl(req.getFormUrl());
        record.setEditUrl(req.getEditUrl());
        record.setIsActive(true);
        formRecordRepo.save(record);

        questionMappingRepo.deleteByFormRecord_Id(formId);

        if (req.getQuestionMapping() != null) {
            req.getQuestionMapping().forEach(item -> {
                UUID mappedQuestionId = UUID.fromString(item.getQuestionId());

                questionRepo.findById(mappedQuestionId).ifPresent(q -> {
                    q.setGoogleItemId(item.getGoogleItemId());
                    questionRepo.save(q);
                });

                FormQuestionMapping mapping = FormQuestionMapping.builder()
                        .formRecord(record)
                        .questionId(mappedQuestionId)
                        .googleItemId(item.getGoogleItemId())
                        .backendSectionId(item.getSectionId())
                        .build();
                questionMappingRepo.save(mapping);
            });
        }
    }

    @Transactional
    public WebhookSubmitResponse processWebhookSubmit(WebhookSubmitRequest req) {
        if (req == null || req.getGoogleFormId() == null || req.getGoogleFormId().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_FORM_NOT_FOUND);
        }

        GoogleFormRecord formRecord = formRecordRepo.findByGoogleFormId(req.getGoogleFormId().trim())
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_FORM_NOT_FOUND));

        Map<String, UUID> itemToQuestion = buildItemToQuestionMap(formRecord.getId());

        Account account = null;
        if (req.getSubmitterEmail() != null && !req.getSubmitterEmail().isBlank()) {
            account = accountRepository.findByEmail(req.getSubmitterEmail().trim()).orElse(null);
        }

        FeedbackSubmission submission = FeedbackSubmission.builder()
                .account(account)
                .curriculum(formRecord.getCurriculum())
                .build();
        submission = submissionRepo.save(submission);

        int processed = 0;
        if (req.getAnswers() != null) {
            for (WebhookSubmitRequest.AnswerPayload answerPayload : req.getAnswers()) {
                UUID questionId = itemToQuestion.get(answerPayload.getGoogleItemId());
                if (questionId == null) {
                    continue;
                }

                FeedbackFormQuestion question = questionRepo.findById(questionId)
                        .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));

                FeedbackAnswer answer = FeedbackAnswer.builder()
                        .submission(submission)
                        .question(question)
                        .build();

                String answerValue = answerPayload.getAnswerValue();
                if (answerValue != null && !answerValue.isBlank()) {
                    String itemType = answerPayload.getItemType() == null ? "" : answerPayload.getItemType().trim().toUpperCase();

                    if (isCheckboxPayload(answerValue)) {
                        answer.setAnswerText(answerValue);
                    } else if (isChoiceType(itemType)) {
                        optionRepo.findByQuestion_QuestionIdAndOptionTextIgnoreCase(questionId, answerValue.trim())
                                .ifPresentOrElse(answer::setSelectedOption, () -> answer.setAnswerText(answerValue.trim()));
                    } else {
                        answer.setAnswerText(answerValue.trim());
                    }
                } else {
                    continue;
                }

                answerRepo.save(answer);
                processed++;
            }
        }

        return WebhookSubmitResponse.builder()
                .success(true)
                .submissionId(submission.getId().toString())
                .answersProcessed(processed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> getSubmissions(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);

        return submissionRepo.findByCurriculum_CurriculumId(record.getCurriculum().getCurriculumId())
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackReportResponse generateReport(UUID formId) {
        GoogleFormRecord record = findFormRecord(formId);
        List<FeedbackSubmission> submissions = submissionRepo.findByCurriculum_CurriculumId(record.getCurriculum().getCurriculumId());

        Map<UUID, AggregateQuestion> aggregates = new LinkedHashMap<>();

        for (FeedbackSubmission submission : submissions) {
            if (submission.getFeedbackAnswers() == null) {
                continue;
            }
            for (FeedbackAnswer answer : submission.getFeedbackAnswers()) {
                if (answer.getQuestion() == null || answer.getQuestion().getQuestionId() == null) {
                    continue;
                }

                UUID qid = answer.getQuestion().getQuestionId();
                AggregateQuestion aggregate = aggregates.computeIfAbsent(qid, k -> new AggregateQuestion(
                        qid.toString(),
                        answer.getQuestion().getContent(),
                        answer.getQuestion().getType()
                ));

                if (answer.getSelectedOption() != null && answer.getSelectedOption().getOptionText() != null) {
                    String key = answer.getSelectedOption().getOptionText();
                    aggregate.optionCounts.put(key, aggregate.optionCounts.getOrDefault(key, 0) + 1);
                }

                if (answer.getAnswerText() != null && !answer.getAnswerText().isBlank()) {
                    aggregate.textAnswers.add(answer.getAnswerText());
                    try {
                        aggregate.ratingTotal += Double.parseDouble(answer.getAnswerText().trim());
                        aggregate.ratingCount++;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        List<FeedbackReportResponse.QuestionReport> questionReports = aggregates.values().stream()
                .map(aggregate -> FeedbackReportResponse.QuestionReport.builder()
                        .questionId(aggregate.questionId)
                        .questionText(aggregate.questionText)
                        .type(aggregate.type)
                        .optionCounts(aggregate.optionCounts)
                        .textAnswers(aggregate.textAnswers)
                        .averageRating(aggregate.ratingCount > 0 ? aggregate.ratingTotal / aggregate.ratingCount : null)
                        .build())
                .toList();

        return FeedbackReportResponse.builder()
                .formId(formId.toString())
                .totalSubmissions(submissions.size())
                .questions(questionReports)
                .build();
    }

    private GoogleFormRecord findFormRecord(UUID formId) {
        return formRecordRepo.findById(formId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_FORM_NOT_FOUND));
    }

    private Map<String, UUID> buildItemToQuestionMap(UUID formRecordId) {
        List<FormQuestionMapping> mappings = questionMappingRepo.findByFormRecord_Id(formRecordId);
        Map<String, UUID> itemToQuestion = new HashMap<>();
        for (FormQuestionMapping mapping : mappings) {
            itemToQuestion.put(mapping.getGoogleItemId(), mapping.getQuestionId());
        }
        return itemToQuestion;
    }

    private String buildFormTitle(GoogleFormRecord record) {
        String curriculumName = record.getCurriculum() != null ? record.getCurriculum().getCurriculumName() : "Curriculum";
        return curriculumName + " - Feedback " + record.getFormType();
    }

    private SectionResponse toSectionResponse(FeedbackFormSection section) {
        return SectionResponse.builder()
                .sectionId(section.getSectionId().toString())
                .title(section.getTitle())
                .orderIndex(section.getOrderIndex())
                .afterSectionAction(section.getAfterSectionAction())
                .build();
    }

    private QuestionResponse toQuestionResponse(FeedbackFormQuestion question) {
        return QuestionResponse.builder()
                .questionId(question.getQuestionId().toString())
                .content(question.getContent())
                .type(question.getType())
                .isRequired(question.getIsRequired())
                .build();
    }

    private FormRecordResponse toFormRecordResponse(GoogleFormRecord record) {
        return FormRecordResponse.builder()
                .id(record.getId().toString())
                .curriculumId(record.getCurriculum().getCurriculumId().toString())
                .googleFormId(record.getGoogleFormId())
                .formUrl(record.getFormUrl())
                .formType(record.getFormType())
                .isActive(record.getIsActive())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private FormSubmissionResponse toSubmissionResponse(FeedbackSubmission submission) {
        List<FormSubmissionAnswerResponse> answers = submission.getFeedbackAnswers() == null
                ? List.of()
                : submission.getFeedbackAnswers().stream().map(answer ->
                FormSubmissionAnswerResponse.builder()
                        .id(answer.getId() != null ? answer.getId().toString() : null)
                        .questionId(answer.getQuestion() != null && answer.getQuestion().getQuestionId() != null
                                ? answer.getQuestion().getQuestionId().toString() : null)
                        .questionText(answer.getQuestion() != null ? answer.getQuestion().getContent() : null)
                        .selectedOptionId(answer.getSelectedOption() != null && answer.getSelectedOption().getOptionId() != null
                                ? answer.getSelectedOption().getOptionId().toString() : null)
                        .selectedOptionText(answer.getSelectedOption() != null ? answer.getSelectedOption().getOptionText() : null)
                        .answerText(answer.getAnswerText())
                        .build())
                .toList();

        return FormSubmissionResponse.builder()
                .id(submission.getId() != null ? submission.getId().toString() : null)
                .accountId(submission.getAccount() != null && submission.getAccount().getAccountId() != null
                        ? submission.getAccount().getAccountId().toString() : null)
                .curriculumId(submission.getCurriculum() != null && submission.getCurriculum().getCurriculumId() != null
                        ? submission.getCurriculum().getCurriculumId().toString() : null)
                .submittedAt(submission.getSubmittedAt())
                .answers(answers)
                .build();
    }

    private boolean isChoiceType(String itemType) {
        return "RADIO".equals(itemType) || "DROPDOWN".equals(itemType);
    }

    private boolean isCheckboxPayload(String answerValue) {
        if (answerValue == null) {
            return false;
        }
        String trimmed = answerValue.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return false;
        }
        try {
            objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static class AggregateQuestion {
        String questionId;
        String questionText;
        String type;
        Map<String, Integer> optionCounts = new LinkedHashMap<>();
        List<String> textAnswers = new ArrayList<>();
        double ratingTotal = 0;
        int ratingCount = 0;

        AggregateQuestion(String questionId, String questionText, String type) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.type = type;
        }
    }
}
