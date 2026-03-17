package com.example.smd.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SourceRequest {
    @NotBlank(message = "SOURCE_NAME_REQUIRED")
    String sourceName;
    String type;
    String author;
    String publisher;
    @Min(value = 1800, message = "INVALID_YEAR")
    int publishedYear;
    String isbn;
    String url;
}
