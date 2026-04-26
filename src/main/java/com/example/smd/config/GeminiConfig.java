package com.example.smd.config;

import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GeminiConfig {
    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Double> getEmbedding(String text, String apiUrl) {
        String url = apiUrl + "?key=" + apiKey;

        try {
            // 1. Tạo JSON Body theo chuẩn của Google
            // Cấu trúc: { "model": "...", "content": { "parts": [{ "text": "..." }] } }
            String jsonBody = objectMapper.createObjectNode()
                    .put("model", "models/text-embedding-004")
                    .set("content", objectMapper.createObjectNode()
                            .set("parts", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode().put("text", text))))
                    .toString();

            // 2. Tạo Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // 3. Gọi API (POST)
            String response = restTemplate.postForObject(url, request, String.class);

            // 4. Parse kết quả trả về để lấy mảng số
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode valuesNode = rootNode.path("embedding").path("values");

            List<Double> vector = new ArrayList<>();
            if (valuesNode.isArray()) {
                for (JsonNode val : valuesNode) {
                    vector.add(val.asDouble());
                }
            }
            return vector;

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new AppException(ErrorCode.AI_QUOTA_EXCEEDED);
        } catch (Exception e) {
            log.error("Gemini AI Provider Error. Detail: ", e);
            throw new AppException(ErrorCode.AI_PROVIDER_ERROR);
        }
    }

    public String prompt(String prompt, String apiUrl) {
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

    public String uploadFile(MultipartFile file, String apiUrl) {
        // URL dùng để upload file nhị phân (chú ý uploadType=media)
        String uploadUrl = apiUrl + "?uploadType=media&key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType() != null ? file.getContentType() : "application/pdf"));

        try {
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                return rootNode.path("file").path("uri").asText();
            }
        } catch (Exception e) {
            log.error("Lỗi Upload file lên Gemini: {}", e.getMessage());
            return null;
        }
        return null;
    }

    public String promptWithFile(String prompt, String fileUri, String mimeType, String apiUrl) {
        String finalUrl = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Tạo Part chữ và Part file
        GeminiPart textPart = new GeminiPart(prompt);
        GeminiPart filePart = new GeminiPart(new GeminiFileData(mimeType, fileUri));

        // Đưa vào Request Body
        GeminiRequest requestBody = new GeminiRequest();
        requestBody.setContents(Collections.singletonList(
                new GeminiContent(Arrays.asList(textPart, filePart))
        ));

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(finalUrl, entity, GeminiResponse.class);

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
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeminiRequest {
        private List<GeminiContent> contents;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeminiContent {
        private List<GeminiPart> parts;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeminiPart {
        private String text;

        @JsonProperty("file_data")
        private GeminiFileData fileData;

        // Constructor dùng cho các hàm cũ (chỉ gửi chữ)
        public GeminiPart(String text) {
            this.text = text;
        }

        // Constructor dùng cho tính năng mới (chỉ gửi file)
        public GeminiPart(GeminiFileData fileData) {
            this.fileData = fileData;
        }
    }

    // Thêm class mới tinh này để định nghĩa file
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeminiFileData {
        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("file_uri")
        private String fileUri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        private List<GeminiCandidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiCandidate {
        private GeminiContent content;
    }
}
