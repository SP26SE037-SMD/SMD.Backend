package com.example.smd.dto.response.syllabus;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusStructureResponse {
    String syllabusName;
    String version; // Lấy từ subjectCode hoặc version của Syllabus
    List<ChapterDTO> chapters;

    @Data
    @Builder
    public static class ChapterDTO {
        UUID materialId;
        String chapterTitle; // Title của Material
        List<String> topics; // Danh sách nội dung từ các Block H1, H2
    }
}
