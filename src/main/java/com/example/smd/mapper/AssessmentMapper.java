package com.example.smd.mapper;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.entities.Assessment;
import org.springframework.stereotype.Component;

@Component
public class AssessmentMapper {

    public AssessmentResponse toResponse(Assessment assessment) {
        if (assessment == null) {
            return null;
        }

        return AssessmentResponse.builder()
                .assessmentId(assessment.getAssessmentId())
                .categoryId(assessment.getAssessmentCategory() != null ? assessment.getAssessmentCategory().getCategoryId() : null)
                .categoryName(assessment.getAssessmentCategory() != null ? assessment.getAssessmentCategory().getCategoryName() : null)
                .typeId(assessment.getAssessmentType() != null ? assessment.getAssessmentType().getTypeId() : null)
                .typeName(assessment.getAssessmentType() != null ? assessment.getAssessmentType().getTypeName() : null)
                .syllabusId(assessment.getSyllabus() != null ? assessment.getSyllabus().getSyllabusId() : null)
                .part(assessment.getPart())
                .weight(assessment.getWeight())
                .completionCriteria(assessment.getCompletionCriteria())
                .duration(assessment.getDuration())
                .questionType(assessment.getQuestionType())
                .knowledgeSkill(assessment.getKnowledgeSkill())
                .gradingGuide(assessment.getGradingGuide())
                .note(assessment.getNote())
                .status(assessment.getStatus())
                .createdAt(assessment.getCreatedAt())
                .build();
    }

    public Assessment toEntity(AssessmentRequest request) {
        if (request == null) {
            return null;
        }

        return Assessment.builder()
                .part(request.getPart())
                .weight(request.getWeight())
                .completionCriteria(request.getCompletionCriteria())
                .duration(request.getDuration())
                .questionType(request.getQuestionType())
                .knowledgeSkill(request.getKnowledgeSkill())
                .gradingGuide(request.getGradingGuide())
                .note(request.getNote())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(Assessment assessment, AssessmentRequest request) {
        if (request.getPart() != null) {
            assessment.setPart(request.getPart());
        }
        if (request.getWeight() != null) {
            assessment.setWeight(request.getWeight());
        }
        if (request.getCompletionCriteria() != null) {
            assessment.setCompletionCriteria(request.getCompletionCriteria());
        }
        if (request.getDuration() != null) {
            assessment.setDuration(request.getDuration());
        }
        if (request.getQuestionType() != null) {
            assessment.setQuestionType(request.getQuestionType());
        }
        if (request.getKnowledgeSkill() != null) {
            assessment.setKnowledgeSkill(request.getKnowledgeSkill());
        }
        if (request.getGradingGuide() != null) {
            assessment.setGradingGuide(request.getGradingGuide());
        }
        if (request.getNote() != null) {
            assessment.setNote(request.getNote());
        }
        if (request.getStatus() != null) {
            assessment.setStatus(request.getStatus());
        }
    }
}
