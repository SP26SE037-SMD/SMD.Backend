package com.example.smd.mapper;

import com.example.smd.dto.response.CurriculumGroupSubjectResponse;
import com.example.smd.entities.Curriculum_Group_Subject;
import org.springframework.stereotype.Component;

@Component
public class CurriculumGroupSubjectMapper {

    // Chuyển đổi từ Entity sang DTO Response
    public CurriculumGroupSubjectResponse toResponse(Curriculum_Group_Subject entity) {
        if (entity == null) {
            return null;
        }

        CurriculumGroupSubjectResponse.CurriculumGroupSubjectResponseBuilder builder =
            CurriculumGroupSubjectResponse.builder()
                .id(entity.getId())
                .semester(entity.getSemester());

        // Map Curriculum info
        if (entity.getCurriculum() != null) {
            builder.curriculumId(entity.getCurriculum().getCurriculumId())
                   .curriculumCode(entity.getCurriculum().getCurriculumCode())
                   .curriculumName(entity.getCurriculum().getCurriculumName());
        }

        // Map Group info (có thể null)
        if (entity.getGroup() != null) {
            builder.groupId(entity.getGroup().getGroupId())
                   .groupCode(entity.getGroup().getGroupCode())
                   .groupName(entity.getGroup().getGroupName());
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
