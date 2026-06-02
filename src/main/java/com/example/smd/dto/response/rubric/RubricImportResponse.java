package com.example.smd.dto.response.rubric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả tổng hợp sau khi import Rubric từ file Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricImportResponse {
    /** Tổng số dòng đã đọc (không tính header). */
    private int total;
    /** Số dòng hợp lệ đã được ghi vào DB. */
    private int success;
    /** Số dòng bị bỏ qua do lỗi validate. */
    private int failed;
    /** Tổng số Rubric được tạo mới. */
    private int rubricCreated;
    /** Tổng số RubricCriterion được tạo mới. */
    private int criterionCreated;
    /** Tổng số CriteriaLevel được tạo mới. */
    private int criteriaLevelCreated;
    /** Danh sách chi tiết kết quả từng dòng (chỉ bao gồm dòng FAILED). */
    private List<RubricImportRowResult> errors;
}
