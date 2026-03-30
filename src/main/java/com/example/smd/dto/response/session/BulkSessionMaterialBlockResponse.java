package com.example.smd.dto.response.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkSessionMaterialBlockResponse {

    boolean success;
    UUID sessionId;
    Integer totalMappingsCreated;
    Integer totalMaterialsProcessed;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<MappingError> errors;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MappingError {
        String errorCode;
        String errorMessage;
        UUID materialId;
        UUID blockId;
        Integer idx;
        String details;
    }
}
