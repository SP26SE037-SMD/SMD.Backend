package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO ánh xạ dữ liệu từ từng dòng của file Excel import Assessment.
 *
 * <p>Cấu trúc cột (0-indexed, dòng 1 là header):
 * <pre>
 *   Col 0 : Category          — Tên danh mục (Formative / Summative)
 *   Col 1 : Type              — Loại hình đánh giá (Lab, Quiz, Final, Midterm...)
 *   Col 2 : Part              — Số thứ tự phần (số nguyên)
 *   Col 3 : Weight            — Trọng số (%)
 *   Col 4 : Completion Criteria — Tiêu chí hoàn thành
 *   Col 5 : Duration          — Thời lượng (phút)
 *   Col 6 : Question Type     — Loại câu hỏi
 *   Col 7 : Knowledge Skill   — Kỹ năng/kiến thức đánh giá
 *   Col 8 : Grading Guide     — Hướng dẫn chấm điểm
 *   Col 9 : Note              — Ghi chú
 *   Col 10: CLO-Mapping       — Các mã CLO, cách nhau bằng dấu phẩy (VD: "CLO1, CLO2")
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssessmentImportDTO {

    @ExcelColumn(name = "Category", order = 0, required = true)
    String category;

    @ExcelColumn(name = "Type", order = 1, required = true)
    String type;

    @ExcelColumn(name = "Part", order = 2, required = false)
    String part;

    @ExcelColumn(name = "Weight", order = 3, required = true)
    String weight;

    @ExcelColumn(name = "Completion Criteria", order = 4, required = false)
    String completionCriteria;

    @ExcelColumn(name = "Duration", order = 5, required = false)
    String duration;

    @ExcelColumn(name = "Question Type", order = 6, required = false)
    String questionType;

    @ExcelColumn(name = "Knowledge Skill", order = 7, required = false)
    String knowledgeSkill;

    @ExcelColumn(name = "Grading Guide", order = 8, required = false)
    String gradingGuide;

    @ExcelColumn(name = "Note", order = 9, required = false)
    String note;

    @ExcelColumn(name = "CLO-Mapping", order = 10, required = false)
    String cloMapping;

    /**
     * Số dòng trong file Excel (1-indexed, tính từ dòng header).
     * Không ánh xạ từ cột Excel — được gán thủ công khi đọc file.
     * FE dùng trường này để highlight dòng lỗi chính xác.
     */
    int rowNumber;
}
