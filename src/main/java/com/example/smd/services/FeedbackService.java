package com.example.smd.services;

import com.example.smd.dto.request.feedback.*;
import com.example.smd.dto.response.feedback.*;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedbackService {

    CurriculumFeedbackQuestionRepository questionRepository;
    FeedbackOptionRepository optionRepository;
    FeedbackSubmissionRepository submissionRepository;
    FeedbackAnswerRepository answerRepository;
    AccountRepository accountRepository;
    CurriculumRepository curriculumRepository;

    @Transactional
    public FeedbackQuestionResponse createQuestion(FeedbackQuestionRequest request) {
        validateQuestionRequest(request);

        Curriculum_Feedback_Question question = Curriculum_Feedback_Question.builder()
                .questionNo(resolveQuestionNo(request.getQuestionNo(), request.getFormType()))
                .questionText(request.getQuestionText().trim())
                .questionType(request.getQuestionType().trim())
                .formType(request.getFormType().trim())
                .isRequired(Boolean.TRUE.equals(request.getIsRequired()))
                .build();

        return toQuestionResponse(questionRepository.save(question));
    }

    @Transactional
    public List<FeedbackQuestionResponse> createQuestionsBatch(FeedbackQuestionBatchRequest request) {
        if (request == null || request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_LIST_REQUIRED);
        }

        return request.getQuestions().stream()
                .map(this::createQuestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackQuestionResponse> getAllQuestions(String formType) {
        List<Curriculum_Feedback_Question> questions;
        if (formType == null || formType.isBlank()) {
            questions = questionRepository.findAll().stream()
                    .sorted(Comparator.comparing(Curriculum_Feedback_Question::getFormType, Comparator.nullsLast(String::compareToIgnoreCase))
                            .thenComparing(Curriculum_Feedback_Question::getQuestionNo, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        } else {
            questions = questionRepository.findByFormTypeOrderByQuestionNoAsc(formType.trim());
        }

        return questions.stream().map(this::toQuestionResponse).toList();
    }

    @Transactional(readOnly = true)
    public FeedbackQuestionResponse getQuestionDetail(UUID questionId) {
        Curriculum_Feedback_Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));
        return toQuestionResponse(question);
    }

    @Transactional
    public FeedbackQuestionResponse updateQuestion(UUID questionId, FeedbackQuestionRequest request) {
        validateQuestionRequest(request);

        Curriculum_Feedback_Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));

        question.setQuestionNo(resolveQuestionNo(request.getQuestionNo(), request.getFormType()));
        question.setQuestionText(request.getQuestionText().trim());
        question.setQuestionType(request.getQuestionType().trim());
        question.setFormType(request.getFormType().trim());
        question.setIsRequired(Boolean.TRUE.equals(request.getIsRequired()));

        return toQuestionResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(UUID questionId) {
        Curriculum_Feedback_Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));

        if (question.getFeedbackAnswers() != null && !question.getFeedbackAnswers().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_IN_USE);
        }

        questionRepository.delete(question);
    }

    @Transactional
    public FeedbackOptionResponse createOption(FeedbackOptionRequest request) {
        validateOptionRequest(request);

        Options option = Options.builder()
                .optionNo(resolveOptionNo(request.getOptionNo()))
                .optionText(request.getOptionText().trim())
                .build();

        return toOptionResponse(optionRepository.save(option));
    }

    @Transactional
    public List<FeedbackOptionResponse> createOptionsBatch(FeedbackOptionBatchRequest request) {
        if (request == null || request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_OPTION_LIST_REQUIRED);
        }

        return request.getOptions().stream()
                .map(this::createOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackOptionResponse> getAllOptions() {
        return optionRepository.findAllByOrderByOptionNoAsc().stream()
                .map(this::toOptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackOptionResponse getOptionDetail(UUID optionId) {
        Options option = optionRepository.findById(optionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_OPTION_NOT_FOUND));
        return toOptionResponse(option);
    }

    @Transactional
    public FeedbackOptionResponse updateOption(UUID optionId, FeedbackOptionRequest request) {
        validateOptionRequest(request);

        Options option = optionRepository.findById(optionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_OPTION_NOT_FOUND));

        option.setOptionNo(resolveOptionNo(request.getOptionNo()));
        option.setOptionText(request.getOptionText().trim());

        return toOptionResponse(optionRepository.save(option));
    }

    @Transactional
    public void deleteOption(UUID optionId) {
        Options option = optionRepository.findById(optionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_OPTION_NOT_FOUND));

        if (option.getFeedbackAnswers() != null && !option.getFeedbackAnswers().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_OPTION_IN_USE);
        }

        optionRepository.delete(option);
    }

    @Transactional(readOnly = true)
    public FeedbackFormResponse getForm(String formType) {
        if (formType == null || formType.isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_FORM_TYPE_REQUIRED);
        }

        List<FeedbackQuestionResponse> questions = questionRepository
                .findByFormTypeOrderByQuestionNoAsc(formType.trim())
                .stream()
                .map(this::toQuestionResponse)
                .toList();

        List<FeedbackOptionResponse> options = optionRepository.findAllByOrderByOptionNoAsc().stream()
                .map(this::toOptionResponse)
                .toList();

        return FeedbackFormResponse.builder()
                .formType(formType.trim())
                .questions(questions)
                .options(options)
                .build();
    }

    @Transactional
    public FeedbackSubmissionResponse createSubmission(FeedbackSubmissionRequest request) {
        validateSubmissionRequest(request);

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        FeedbackSubmissions submission = FeedbackSubmissions.builder()
                .account(account)
                .curriculum(curriculum)
                .build();

        submission = submissionRepository.save(submission);

        List<FeedbackAnswers> answersToSave = new ArrayList<>();
        for (FeedbackAnswerRequest answerRequest : request.getAnswers()) {
            if (answerRequest.getQuestionId() == null) {
                throw new AppException(ErrorCode.FEEDBACK_QUESTION_ID_REQUIRED);
            }

            Curriculum_Feedback_Question question = questionRepository.findById(answerRequest.getQuestionId())
                    .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_QUESTION_NOT_FOUND));

            Options selectedOption = null;
            if (answerRequest.getSelectedOptionId() != null) {
                selectedOption = optionRepository.findById(answerRequest.getSelectedOptionId())
                        .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_OPTION_NOT_FOUND));
            }

            String answerText = answerRequest.getAnswerText();
            boolean hasText = answerText != null && !answerText.isBlank();
            if (selectedOption == null && !hasText) {
                throw new AppException(ErrorCode.FEEDBACK_ANSWER_INVALID);
            }

            FeedbackAnswers answer = FeedbackAnswers.builder()
                    .feedbackSubmission(submission)
                    .question(question)
                    .selectedOption(selectedOption)
                    .answerText(hasText ? answerText.trim() : null)
                    .build();

            answersToSave.add(answer);
        }

        List<FeedbackAnswers> savedAnswers = answerRepository.saveAll(answersToSave);
        submission.setFeedbackAnswers(savedAnswers);

        return toSubmissionResponse(submission);
    }

    @Transactional
    public List<FeedbackSubmissionResponse> createSubmissionBatch(FeedbackSubmissionBatchRequest request) {
        if (request == null || request.getSubmissions() == null || request.getSubmissions().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_SUBMISSION_LIST_REQUIRED);
        }

        return request.getSubmissions().stream()
                .map(this::createSubmission)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackSubmissionResponse getSubmissionDetail(UUID submissionId) {
        FeedbackSubmissions submission = submissionRepository.findDetailedById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_SUBMISSION_NOT_FOUND));
        return toSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<FeedbackSubmissionResponse> getSubmissionsByCurriculum(UUID curriculumId) {
        if (!curriculumRepository.existsById(curriculumId)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        return submissionRepository.findByCurriculum_CurriculumId(curriculumId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackSubmissionResponse> getSubmissionsByAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        return submissionRepository.findByAccount_AccountId(accountId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse getSummaryByCurriculum(UUID curriculumId) {
        if (!curriculumRepository.existsById(curriculumId)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        List<FeedbackSubmissions> submissions = submissionRepository.findByCurriculum_CurriculumId(curriculumId);
        List<FeedbackAnswers> allAnswers = submissions.stream()
                .filter(Objects::nonNull)
                .flatMap(submission -> submission.getFeedbackAnswers() == null
                        ? java.util.stream.Stream.empty()
                        : submission.getFeedbackAnswers().stream())
                .toList();

        Map<UUID, List<FeedbackAnswers>> groupedByQuestion = allAnswers.stream()
                .filter(answer -> answer.getQuestion() != null && answer.getQuestion().getId() != null)
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        List<FeedbackQuestionSummaryResponse> questionSummaries = groupedByQuestion.values().stream()
                .map(this::toQuestionSummary)
                .sorted(Comparator.comparing(FeedbackQuestionSummaryResponse::getQuestionText, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return FeedbackSummaryResponse.builder()
                .curriculumId(curriculumId.toString())
                .totalSubmissions(submissions.size())
                .questions(questionSummaries)
                .build();
    }

    private FeedbackQuestionSummaryResponse toQuestionSummary(List<FeedbackAnswers> answers) {
        FeedbackAnswers sample = answers.get(0);
        Curriculum_Feedback_Question question = sample.getQuestion();

        Map<UUID, List<FeedbackAnswers>> byOption = answers.stream()
                .filter(answer -> answer.getSelectedOption() != null && answer.getSelectedOption().getId() != null)
                .collect(Collectors.groupingBy(answer -> answer.getSelectedOption().getId()));

        List<FeedbackOptionStatResponse> optionStats = byOption.values().stream()
                .map(optionAnswers -> {
                    Options option = optionAnswers.get(0).getSelectedOption();
                    return FeedbackOptionStatResponse.builder()
                            .optionId(option.getId().toString())
                            .optionText(option.getOptionText())
                            .totalSelected(optionAnswers.size())
                            .build();
                })
                .sorted(Comparator.comparing(FeedbackOptionStatResponse::getOptionText, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        long textAnswerCount = answers.stream()
                .map(FeedbackAnswers::getAnswerText)
                .filter(text -> text != null && !text.isBlank())
                .count();

        return FeedbackQuestionSummaryResponse.builder()
                .questionId(question.getId().toString())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .totalAnswers(answers.size())
                .optionStats(optionStats)
                .textAnswerCount(textAnswerCount)
                .build();
    }

    private FeedbackQuestionResponse toQuestionResponse(Curriculum_Feedback_Question question) {
        return FeedbackQuestionResponse.builder()
                .id(question.getId() != null ? question.getId().toString() : null)
                .questionNo(question.getQuestionNo())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .formType(question.getFormType())
                .isRequired(question.getIsRequired())
                .createdAt(question.getCreatedAt())
                .build();
    }

    private FeedbackOptionResponse toOptionResponse(Options option) {
        return FeedbackOptionResponse.builder()
                .id(option.getId() != null ? option.getId().toString() : null)
                .optionNo(option.getOptionNo())
                .optionText(option.getOptionText())
                .build();
    }

    private FeedbackSubmissionResponse toSubmissionResponse(FeedbackSubmissions submission) {
        List<FeedbackAnswerResponse> answers = submission.getFeedbackAnswers() == null
                ? List.of()
                : submission.getFeedbackAnswers().stream()
                .map(answer -> FeedbackAnswerResponse.builder()
                        .id(answer.getId() != null ? answer.getId().toString() : null)
                        .questionId(answer.getQuestion() != null && answer.getQuestion().getId() != null ? answer.getQuestion().getId().toString() : null)
                        .questionText(answer.getQuestion() != null ? answer.getQuestion().getQuestionText() : null)
                        .selectedOptionId(answer.getSelectedOption() != null && answer.getSelectedOption().getId() != null
                                ? answer.getSelectedOption().getId().toString()
                                : null)
                        .selectedOptionText(answer.getSelectedOption() != null ? answer.getSelectedOption().getOptionText() : null)
                        .answerText(answer.getAnswerText())
                        .build())
                .toList();

        return FeedbackSubmissionResponse.builder()
                .id(submission.getId() != null ? submission.getId().toString() : null)
                .accountId(submission.getAccount() != null && submission.getAccount().getAccountId() != null
                        ? submission.getAccount().getAccountId().toString()
                        : null)
                .curriculumId(submission.getCurriculum() != null && submission.getCurriculum().getCurriculumId() != null
                        ? submission.getCurriculum().getCurriculumId().toString()
                        : null)
                .submittedAt(submission.getSubmittedAt())
                .answers(answers)
                .build();
    }

    private void validateQuestionRequest(FeedbackQuestionRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_REQUIRED);
        }
        if (request.getQuestionText() == null || request.getQuestionText().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_TEXT_REQUIRED);
        }
        if (request.getQuestionType() == null || request.getQuestionType().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_QUESTION_TYPE_REQUIRED);
        }
        if (request.getFormType() == null || request.getFormType().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_FORM_TYPE_REQUIRED);
        }
    }

    private void validateOptionRequest(FeedbackOptionRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.FEEDBACK_OPTION_REQUIRED);
        }
        if (request.getOptionText() == null || request.getOptionText().isBlank()) {
            throw new AppException(ErrorCode.FEEDBACK_OPTION_TEXT_REQUIRED);
        }
    }

    private void validateSubmissionRequest(FeedbackSubmissionRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.FEEDBACK_SUBMISSION_REQUIRED);
        }
        if (request.getAccountId() == null) {
            throw new AppException(ErrorCode.ACCOUNT_ID_REQUIRED);
        }
        if (request.getCurriculumId() == null) {
            throw new AppException(ErrorCode.FEEDBACK_CURRICULUM_ID_REQUIRED);
        }
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new AppException(ErrorCode.FEEDBACK_ANSWER_LIST_REQUIRED);
        }
    }

    private Integer resolveQuestionNo(Integer questionNo, String formType) {
        if (questionNo != null) {
            return questionNo;
        }

        List<Curriculum_Feedback_Question> existing = questionRepository.findByFormTypeOrderByQuestionNoAsc(formType.trim());
        if (existing.isEmpty()) {
            return 1;
        }

        Integer maxNo = existing.get(existing.size() - 1).getQuestionNo();
        return (maxNo == null ? 1 : maxNo + 1);
    }

    private Integer resolveOptionNo(Integer optionNo) {
        if (optionNo != null) {
            return optionNo;
        }

        List<Options> existing = optionRepository.findAllByOrderByOptionNoAsc();
        if (existing.isEmpty()) {
            return 1;
        }

        Integer maxNo = existing.get(existing.size() - 1).getOptionNo();
        return (maxNo == null ? 1 : maxNo + 1);
    }
}
