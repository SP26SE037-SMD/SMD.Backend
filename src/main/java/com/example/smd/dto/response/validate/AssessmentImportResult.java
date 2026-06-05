package com.example.smd.dto.response.validate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả trả về sau khi import Assessment từ file Excel.
 * <ul>
 *   <li>{@code isValid = true}  → toàn bộ validate pass, dữ liệu đã được lưu DB.</li>
 *   <li>{@code isValid = false} → có lỗi, trả HTTP 400, KHÔNG lưu DB.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentImportResult {

    /** Trạng thái tổng: true khi không có lỗi nào. */
    @Builder.Default
    private boolean isValid = true;

    /** Số dòng đọc được từ file Excel (bỏ qua "ghost rows"). */
    private int totalRows;

    /** Số Assessment đã lưu thành công (chỉ có ý nghĩa khi isValid = true). */
    private int savedCount;

    /** Danh sách lỗi chi tiết — rỗng nếu isValid = true. */
    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    // ─── Helper Methods ──────────────────────────────────────────────── //

    /**
     * Thêm lỗi liên quan đến một dòng cụ thể trong file Excel.
     *
     * @param code      Mã lỗi ngắn gọn (VD: "CLO_INVALID", "CATEGORY_TYPE_MISMATCH")
     * @param message   Mô tả lỗi dạng văn bản tự nhiên
     * @param rowNumber Số dòng Excel gây lỗi (1-indexed); dùng -1 nếu là lỗi tổng quan
     */
    public void addError(String code, String message, int rowNumber) {
        this.errors.add(new ImportError(code, message, rowNumber));
        this.isValid = false;
    }

    // ─── Inner Classes ───────────────────────────────────────────────── //

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportError {

        /**
         * Mã lỗi định danh — VD:
         * <ul>
         *   <li>{@code CATEGORY_TYPE_MISMATCH} — Sai cặp Category/Type</li>
         *   <li>{@code CLO_INVALID}            — Mã CLO không thuộc Subject</li>
         *   <li>{@code WEIGHT_NOT_100}          — Tổng trọng số ≠ 100</li>
         *   <li>{@code MISSING_REQUIRED_FIELD}  — Trường bắt buộc bị trống</li>
         *   <li>{@code FILE_READ_ERROR}         — Không đọc được file</li>
         * </ul>
         */
        private String code;

        /** Mô tả lỗi dạng văn bản, hiển thị trực tiếp cho người dùng. */
        private String message;

        /**
         * Số dòng Excel gây ra lỗi (1-indexed).
         * Giá trị {@code -1} biểu thị lỗi tổng quan (không thuộc dòng cụ thể).
         */
        private int rowNumber;
    }
}
