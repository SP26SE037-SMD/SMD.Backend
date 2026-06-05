package com.example.smd.dto.response.validate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả trả về sau khi import Session từ Excel.
 * Kế thừa cấu trúc từ SessionValidationResult và bổ sung thêm:
 *  - Danh sách Session đã lưu thành công (khi isValid = true)
 *  - Số dòng bị lỗi (để FE hiển thị tóm tắt)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionImportResult {

    /** true = toàn bộ validate pass, dữ liệu đã được lưu DB */
    @Builder.Default
    private boolean isValid = true;

    /** Danh sách lỗi chi tiết — rỗng nếu isValid = true */
    @Builder.Default
    private List<ImportError> importErrors = new ArrayList<>();

    private SessionValidationResult validateError;

    /** Số dòng đọc được từ file Excel */
    private int totalRows;

    /** Số Session đã lưu thành công (chỉ có ý nghĩa khi isValid = true) */
    private int savedCount;

    // ------------------------------------------------------------------ //

    public void addError(String code, String message, Integer rowIndex) {
        this.importErrors.add(new ImportError(code, message, rowIndex));
        this.isValid = false;
    }

    /** Merge toàn bộ lỗi từ SessionValidationResult (validate quota) vào result hiện tại */
    public void mergeErrors(SessionValidationResult quotaResult) {
        if (!quotaResult.isValid()) {
            this.validateError = quotaResult;
            this.isValid = false;
        }
    }

    // ===================== INNER CLASSES ===================== //

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportError {
        /** Mã lỗi — VD: "CLO_INVALID", "THEORY_SURPLUS", "MISSING_FIELD" */
        private String code;

        /** Mô tả lỗi bằng ngôn ngữ tự nhiên */
        private String message;

        /**
         * Số dòng Excel gây ra lỗi (1-indexed, null nếu là lỗi tổng quan như quota).
         * FE dùng trường này để highlight dòng lỗi.
         */
        private Integer rowIndex;
    }
}
