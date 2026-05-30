package com.example.smd.dto.response.pdf;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin một môn học được trích xuất từ section
 * "7. Program content" của file PDF chương trình đào tạo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PdfSubjectExtractDTO {

    /** Mã môn học (Subject Code), ví dụ: 001535 */
    String subjectCode;

    /** Tên môn học (Subject Name), ví dụ: Marxist-Leninist philosophy */
    String subjectName;

    /** Học kỳ dự kiến (Expected Semester), ví dụ: 1 */
    String expectedSemester;

    /** Số tín chỉ (Number of Credits), ví dụ: 3 */
    String numberOfCredits;

    /** Số tiết lý thuyết (Theory periods), ví dụ: 33 */
    String theory;

    /** Số tiết thực hành (Practical periods), ví dụ: 24 */
    String practical;

    /** Số tiết tự học (Self-study periods), ví dụ: 90 */
    String selfStudy;

    /**
     * Chuyển đổi sang định dạng chuỗi theo quy tắc đầu ra:
     * SubjectName (SubjectCode|ExpectedSemester|Credits|Theory|Practical|SelfStudy)
     */
    public String toFormattedString() {
        return String.format("%s (%s|%s|%s|%s|%s|%s)",
                subjectName, subjectCode, expectedSemester,
                numberOfCredits, theory, practical, selfStudy);
    }
}
