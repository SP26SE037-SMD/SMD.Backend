package com.example.smd.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class GeminiConfig {
    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateClo(String prompt, String apiUrl) {
        // 1. Tạo URL có chứa API Key (Gemini dùng key trên URL)
        String finalUrl = apiUrl + "?key=" + apiKey;

        // 2. Tạo Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. Tạo Body theo chuẩn của Gemini (khác OpenAI)
        // Cấu trúc: { "contents": [{ "parts": [{ "text": "..." }] }] }
        GeminiRequest requestBody = new GeminiRequest();
        requestBody.setContents(Collections.singletonList(
                new GeminiContent(Collections.singletonList(
                        new GeminiPart(prompt)
                ))
        ));

        // 4. Gọi API
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(finalUrl, entity, GeminiResponse.class);

            // 5. Lấy kết quả
            if (response.getBody() != null && !response.getBody().getCandidates().isEmpty()) {
                return response.getBody().getCandidates().get(0).getContent().getParts().get(0).getText();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi gọi Gemini: " + e.getMessage();
        }
        return "Không có phản hồi từ Gemini";
    }

    // =========================================================
    // DTO Class - Cấu trúc JSON của Gemini
    // =========================================================

    @Data
    @NoArgsConstructor
    public static class GeminiRequest {
        private List<GeminiContent> contents;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GeminiContent {
        private List<GeminiPart> parts;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GeminiPart {
        private String text;
    }

    @Data @NoArgsConstructor
    public static class GeminiResponse {
        private List<GeminiCandidate> candidates;
    }

    @Data @NoArgsConstructor
    public static class GeminiCandidate {
        private GeminiContent content;
    }
}
