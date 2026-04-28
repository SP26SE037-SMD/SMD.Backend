package com.example.smd.dto.response.validate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentValidationResult {

    // Mặc định là true, sẽ tự động chuyển thành false nếu gọi hàm addError
    @Builder.Default
    private boolean isValid = true;

    // Danh sách các lỗi để Frontend render màu đỏ
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    // Tóm tắt tình trạng hiện tại để Frontend làm UI (VD: Progress Bar)
    private AssessmentSummary summary;

    // --- Hàm Helper ---
    public void addError(String code, String message) {
        this.errors.add(new ValidationError(code, message));
        this.isValid = false;
    }

    // ================= CLASS CON (INNER CLASSES) ================= //

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationError {
        private String code;     // VD: "WEIGHT_SHORTAGE", "MISSING_FINAL_ASSESSMENT"
        private String message;  // VD: "Total weight is short by 20%..."
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssessmentSummary {
        private double currentTotalWeight;       // Tổng trọng số hiện tại (VD: 80)
        private boolean hasFinalAssessment;   // Đã có bài thi Cuối kỳ chưa?
        private boolean hasFormativeAssessment; // Đã có bài đánh giá Quá trình chưa?

        private long totalAssessmentCount;    // Tổng số lượng bài đánh giá
        private long formativeCount;          // Số lượng bài Quá trình
        private long finalCount;              // Số lượng bài Tổng kết (Cuối kỳ)
    }
}
