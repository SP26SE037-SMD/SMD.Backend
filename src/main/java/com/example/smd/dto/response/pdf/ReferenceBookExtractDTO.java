package com.example.smd.dto.response.pdf;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin một tài liệu tham khảo (Reference Book) được trích xuất
 * từ section "*List of Reference Books" của file PDF chương trình đào tạo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferenceBookExtractDTO {

    /** Mã tài liệu tham khảo, ví dụ: 000100 */
    String referenceCode;

    /** Tên tài liệu, ví dụ: Giáo trình những nguyên lý cơ bản chủ nghĩa Mác - Lênin */
    String referenceName;

    /** Tên tác giả (nhiều tác giả ngăn cách bằng " / "), ví dụ: Bộ GD ĐT */
    String authorName;

    /** Nhà xuất bản, ví dụ: NXB Chính trị quốc gia - Sự thật */
    String publisher;

    /** Năm xuất bản, ví dụ: 2016 */
    String publishedYear;

    /**
     * Mã môn học liên quan (có thể nhiều mã, ngăn cách bằng dấu phẩy).
     * Ví dụ: "001535" hoặc "001535,001536"
     */
    String subjectCode;

    /**
     * Chuyển đổi sang định dạng chuỗi theo quy tắc đầu ra:
     * ReferenceName (ReferenceCode|SubjectCode|AuthorName|Publisher|PublishedYear)
     */
    public String toFormattedString() {
        return String.format("%s (%s|%s|%s|%s|%s)",
                referenceName, referenceCode, subjectCode,
                authorName, publisher, publishedYear);
    }
}
