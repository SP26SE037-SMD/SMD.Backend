package com.example.smd.mapper;

import com.example.smd.dto.response.CurriculumComboSubjectResponse;
import com.example.smd.entities.Curriculum_Combo_Subject;
import org.springframework.stereotype.Component;

@Component
public class CurriculumComboSubjectMapper {

    // Chuyển đổi từ Entity sang DTO Response
    public CurriculumComboSubjectResponse toResponse(Curriculum_Combo_Subject entity) {
        if (entity == null) {
            return null;
        }

        CurriculumComboSubjectResponse.CurriculumComboSubjectResponseBuilder builder =
            CurriculumComboSubjectResponse.builder()
                .id(entity.getId())
                .semester(entity.getSemester());

        // Map Curriculum info
        if (entity.getCurriculum() != null) {
            builder.curriculumId(entity.getCurriculum().getCurriculumId())
                   .curriculumCode(entity.getCurriculum().getCurriculumCode())
                   .curriculumName(entity.getCurriculum().getCurriculumName());
        }

        // Map Combo info (có thể null)
        if (entity.getCombo() != null) {
            builder.comboId(entity.getCombo().getComboId())
                   .comboCode(entity.getCombo().getComboCode())
                   .comboName(entity.getCombo().getComboName());
        }

        // Map Subject info
        if (entity.getSubject() != null) {
            builder.subjectId(entity.getSubject().getSubjectId())
                   .subjectCode(entity.getSubject().getSubjectCode())
                   .subjectName(entity.getSubject().getSubjectName());
        }

        return builder.build();
    }
}
