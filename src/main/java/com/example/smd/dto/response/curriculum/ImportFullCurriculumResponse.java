package com.example.smd.dto.response.curriculum;

import com.example.smd.dto.response.curriculumgroupsubject.ImportCurriculumGroupSubjectResponse;
import com.example.smd.dto.response.group.ImportGroupResponse;
import com.example.smd.dto.response.major.ImportMajorResponse;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportFullCurriculumResponse {
    boolean success;
    String message;
    
    ImportMajorResponse majorResult;
    ImportCurriculumResponse curriculumResult;
    ImportSubjectResponse subjectResult;
    ImportGroupResponse groupResult;
    ImportCurriculumGroupSubjectResponse semesterMappingResult;
    com.example.smd.dto.response.source.ImportSourceResponse sourceResult;
}
