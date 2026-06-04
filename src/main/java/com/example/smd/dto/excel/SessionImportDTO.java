package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO dùng để hứng dữ liệu được đọc từ từng dòng của file Excel import Session.
 * Các cột tương ứng trong file Excel:
 *  - Session Number  : Số thứ tự buổi học (bắt buộc)
 *  - Title           : Tiêu đề/Chương của buổi học (bắt buộc)
 *  - Teaching Methods: Phương pháp giảng dạy
 *  - Topic           : Nội dung/Chủ đề của buổi
 *  - Type            : Loại buổi học (THEORY / PRACTICE / SELF_STUDY)
 *  - CLO-Mapping     : Danh sách mã CLO, cách nhau bằng dấu phẩy (VD: "CLO1, CLO2")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionImportDTO {

    @ExcelColumn(name = "Session Number", order = 0, required = true)
    /** Số thứ tự buổi học — tương ứng với cột "Session Number" */
    Integer sessionNumber;

    @ExcelColumn(name = "Title", order = 1, required = true)
    /** Tiêu đề buổi học — tương ứng với cột "Title" */
    String sessionTitle;

    @ExcelColumn(name = "Teaching Methods", order = 2, required = true)
    /** Phương pháp giảng dạy — tương ứng với cột "Teaching Methods" */
    String teachingMethods;

    @ExcelColumn(name = "Topic", order = 3, required = true)
    /** Nội dung / chủ đề — tương ứng với cột "Topic" */
    String sessionTopic;

    @ExcelColumn(name = "Type", order = 4, required = true)
    /** Loại buổi học (THEORY / PRACTICE / SELF_STUDY) — tương ứng với cột "Type" */
    String sessionType;

    @ExcelColumn(name = "CLO-Mapping", order = 5, required = true)
    /**
     * Danh sách mã CLO đã được tách sẵn (trim, uppercase) từ cột "CLO-Mapping".
     * Ví dụ: ["CLO1", "CLO2"]
     */
    List<String> cloCodes;

    /** Số dòng trong file Excel (1-indexed) — dùng để báo lỗi chính xác cho người dùng */
    int rowIndex;
}
