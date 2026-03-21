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
    Role: Bạn là chuyên gia thẩm định giáo trình (Curriculum Auditor).
    
    Nhiệm vụ: So sánh 2 cấu trúc môn học dưới đây để tìm ra SỰ THAY ĐỔI VỀ KIẾN THỨC (Knowledge Delta).
    
    --- OLD SYLLABUS (Môn Cũ) ---
    %s
    
    --- NEW SYLLABUS (Môn Mới) ---
    %s
    
    Yêu cầu Output: Trả về duy nhất 1 JSON object (không markdown, không giải thích) theo định dạng:
    {
      "removed_concepts": ["List các concept kỹ thuật bị xóa"],
      "added_concepts": ["List các concept mới thêm vào"],
      "risk_assessment": "HIGH/MEDIUM/LOW",
      "risk_reason": "Giải thích ngắn gọn tại sao rủi ro."
    }
    """;
}
