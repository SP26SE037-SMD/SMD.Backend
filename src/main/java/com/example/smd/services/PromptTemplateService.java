package com.example.smd.services;

public class PromptTemplateService {

    // Template cho việc tạo CLO
    public static final String CLO_GENERATOR =
            """
            Role: ABET Accreditation Expert & Syllabus Designer.
            Task: Write exactly ONE Course Learning Outcome (CLO) in English that contributes to the specific Program Learning Outcome (PLO) provided.
            
            Context:
            - Subject: %s
            - Topic: %s
            - Bloom Level: %d
            - Related PLO Objective: %s
            
            Rules:
            1. Start with a strong action verb (according to the specified Bloom Level).
            2. The content must clearly demonstrate how it contributes to the given PLO.
            3. Be concise and professional; do not use subjects (e.g., "Students will...").
            4. Language: Use English only.
            
            Format Output: Return ONLY a JSON object with the following structure:
            {
              "cloName": "Short and concise name for the CLO",
              "description": "Full description starting with a verb"
            }
            
            Current Requirement:
            - Output:
            """;

    // Template cho việc check CLO
    public static final String VALIDATOR_PROMPT =
            "Role: Bạn là chuyên gia kiểm định chất lượng giáo dục (QA).\n" +
                    "Nhiệm vụ: Kiểm tra xem CLO sau đây có khớp với Thang đo Bloom mong muốn không.\n" +
                    "Input:\n" +
                    "- CLO: \"%s\"\n" +
                    "- Mức mong muốn: %d\n" +
                    "Yêu cầu Output: Trả về duy nhất 1 JSON object (không markdown, không giải thích) theo định dạng:\n" +
                    "{\n" +
                    "  \"valid\": true/false,\n" +
                    "  \"detectedVerb\": \"...\",\n" +
                    "  \"detectedLevel\": \"...\",\n" +
                    "  \"suggestion\": \"...\"\n" +
                    "}";

    public static final String COMPARISON_PROMPT = """
Role: Bạn là chuyên gia thẩm định giáo trình (Curriculum Auditor) cấp cao.

Nhiệm vụ: Phân tích sâu 2 cấu trúc JSON của giáo trình dưới đây. Hãy xác định những khái niệm kiến thức (concepts) nào đã bị loại bỏ và những khái niệm nào mới được đưa vào.

--- DỮ LIỆU GIÁO TRÌNH ---
[OLD SYLLABUS]:
%s

[NEW SYLLABUS]:
%s

Tiêu chuẩn phân tích:
1. Tập trung vào các thuật ngữ chuyên môn, công nghệ và nguyên lý (ví dụ: Dependency Injection, SOLID, Java I/O).
2. Nếu một khái niệm chỉ được viết lại bằng câu chữ khác nhưng ý nghĩa không đổi, ĐỪNG liệt kê vào Delta.
3. 'risk_assessment' đánh giá dựa trên việc: Nếu sinh viên học môn cũ mà không có môn mới, họ sẽ bị hổng bao nhiêu kiến thức nền tảng quan trọng.

Yêu cầu Output: Trả về DUY NHẤT 1 JSON object (không ```json, không text thừa) đúng cấu trúc:
{
  "removed_concepts": ["concept A", "concept B"],
  "added_concepts": ["concept C", "concept D"],
  "risk_assessment": "HIGH/MEDIUM/LOW",
  "risk_reason": "Giải thích ngắn gọn bằng tiếng Việt về tác động của sự thay đổi."
}
""";

    public static final String DETERMINE_IMPACT = """
            Bạn là một chuyên gia phân tích chương trình đào tạo (Syllabus Analyst).
            Tôi có một khái niệm kiến thức bị thiếu từ môn học trước là: "%s".
            Dưới đây là nội dung tìm thấy trong môn học hiện tại:
            ---
            %s
            ---
            Dựa trên nội dung trên, hãy phân loại mối quan hệ của khái niệm này với môn học hiện tại theo 2 nhãn sau:
            
            1. 'RESOLVED': Nếu nội dung trên thực sự giảng dạy, giải thích định nghĩa hoặc hướng dẫn chi tiết lại khái niệm này cho sinh viên. (Đóng vai trò là phần bù đắp kiến thức).
            
            2. 'REQUIRED': Nếu nội dung trên chỉ yêu cầu sinh viên sử dụng, áp dụng khái niệm này để làm bài tập/dự án mà không giảng dạy lại lý thuyết. (Đóng vai trò là điều kiện tiên quyết).

            Chỉ trả về duy nhất một từ là 'RESOLVED' hoặc 'REQUIRED'. Không giải thích gì thêm.
            """;
}
