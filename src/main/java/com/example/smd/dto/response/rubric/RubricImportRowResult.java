package com.example.smd.dto.response.rubric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chi tiết kết quả của từng dòng Excel trong quá trình import Rubric.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricImportRowResult {
    /** Số thứ tự dòng trong file Excel (bắt đầu từ 2 — bỏ qua header). */
    private int rowNumber;
    /** Rubric Code của dòng đó (nếu đọc được). */
    private String rubricCode;
    /** Criteria Code của dòng đó (nếu đọc được). */
    private String criteriaCode;
    /** "SUCCESS" hoặc "FAILED". */
    private String status;
    /** Mô tả lỗi chi tiết (null nếu thành công). */
    private String message;
}
