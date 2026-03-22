package com.example.smd.dto.response.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskCurriculumResponse {

    UUID curriculumId;

    @JsonProperty("curriculum_code")
    String curriculumCode;

    @JsonProperty("curriculum_name")
    String curriculumName;

    @JsonProperty("start_year")
    Integer startYear;

    String status;
}
