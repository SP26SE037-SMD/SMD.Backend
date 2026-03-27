package com.example.smd.mapper;

import com.example.smd.dto.response.clo.CloAssessmentMappingResponse;
import com.example.smd.entities.Assessment;
import com.example.smd.entities.CLO_Assessment;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Syllabus;
import org.springframework.stereotype.Component;

@Component
public class CloAssessmentMappingMapper {

    public CloAssessmentMappingResponse toResponse(CLO_Assessment mapping) {
        if (mapping == null) {
            return null;
        }

        CLOs clo = mapping.getClo();
        Assessment assessment = mapping.getAssessment();
        Syllabus syllabus = assessment != null ? assessment.getSyllabus() : null;

        return CloAssessmentMappingResponse.builder()
                .id(mapping.getId() != null ? mapping.getId().toString() : null)
                .cloId(clo != null && clo.getCloId() != null ? clo.getCloId().toString() : null)
                .cloCode(clo != null ? clo.getCloCode() : null)
                .assessmentId(assessment != null && assessment.getAssessmentId() != null
                        ? assessment.getAssessmentId().toString()
                        : null)
                .assessmentPart(assessment != null ? assessment.getPart() : null)
                .assessmentStatus(assessment != null ? assessment.getStatus() : null)
                .syllabusId(syllabus != null && syllabus.getSyllabusId() != null
                        ? syllabus.getSyllabusId().toString()
                        : null)
                .build();
    }
}
