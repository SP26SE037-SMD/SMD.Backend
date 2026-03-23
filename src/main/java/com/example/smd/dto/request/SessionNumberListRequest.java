package com.example.smd.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class SessionNumberListRequest {

    @NotEmpty(message = "SESSION_NUMBER_LIST_REQUIRED")
    @Valid
    List<@NotNull(message = "SESSION_NUMBER_REQUIRED") @Min(value = 1, message = "SESSION_NUMBER_INVALID") Integer> sessionNumbers;
}
