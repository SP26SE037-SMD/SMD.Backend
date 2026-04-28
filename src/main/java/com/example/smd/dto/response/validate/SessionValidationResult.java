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
public class SessionValidationResult {

    // Trạng thái chung: true nếu không có lỗi nào, false nếu có lỗi
    @Builder.Default
    private boolean isValid = true;

    // Danh sách các lỗi (hoặc cảnh báo)
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    // Thống kê số tiết còn dư/thiếu để Frontend hiển thị UI
    private RemainingQuota remainingQuotas;

    // --- Các hàm Helper để Dev dễ dàng thêm lỗi trong lúc viết Logic ---

    public void addError(String code, String message) {
        this.errors.add(new ValidationError(code, message, null));
        this.isValid = false; // Tự động chuyển thành false khi có lỗi
    }

    public void addError(String code, String message, Integer week) {
        this.errors.add(new ValidationError(code, message, week));
        this.isValid = false;
    }

    // ================= CLASS CON (INNER CLASSES) ================= //

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationError {
        private String code;     // Ví dụ: "THEORY_MISMATCH", "WEEKLY_OVERLOAD"
        private String message;  // Ví dụ: "Tuần 5 đang có 32 tiết..."
        private Integer week;    // Null nếu lỗi chung, có số nếu lỗi ở một tuần cụ thể
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RemainingQuota {
        private int theory;      // Số tiết lý thuyết còn lại cần phân bổ
        private int practice;    // Số tiết thực hành còn lại cần phân bổ
        private int selfStudy;   // (Tùy chọn) Số giờ tự học còn lại
    }
}
