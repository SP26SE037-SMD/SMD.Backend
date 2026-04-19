package com.example.smd.dto.request;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloPloMappingBulkRequest {
    @Valid
    List<CloPloMappingRequest> deletedMappings;

    @Valid
    List<CloPloMappingRequest> addedMappings;
}
