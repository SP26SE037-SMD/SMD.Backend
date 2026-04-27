package com.example.smd.services;

import com.example.smd.config.GeminiConfig;
import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.request.clo.CloCheckRequest;
import com.example.smd.dto.request.clo.CloGenerationRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ImpactResponse;
import com.example.smd.dto.response.ProgramRegulationResponse;
import com.example.smd.dto.response.clo.CLOsGenerationResponse;
import com.example.smd.dto.response.clo.CloCheckResponse;
import com.example.smd.dto.response.syllabus.SyllabusStructureResponse;
import com.example.smd.entities.Vector_Embeddings;
import com.example.smd.enums.PromptKey;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class GeminiService {

    @Autowired
    private GeminiConfig gemini;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gemini.generate.url}")
    private String apiGenerateUrl;

    @Value("${gemini.check.url}")
    private String apiCheckUrl;

    @Value("${gemini.vector-embedding.url}")
    private String apiEmbeddingUrl;

    @Value("${gemini.upload-file.url}")
    private String apiUploadFileUrl;

    @Value("${gemini.analyze-pdf.url}")
    private String apiAnalyzePdfUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Dùng AI để generate ra CLO
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    public CLOsGenerationResponse generateClo(CloGenerationRequest req, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!"HOPDC".equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Ghép dữ liệu vào Prompt bằng hàm format đã chuẩn bị
        String finalPrompt = String.format(promptTemplateService.get(PromptKey.CLO_GENERATOR),
                req.getSubjectName(),
                req.getTopicName(),
                req.getBloomLevel(),
                req.getDescriptionPlo()
        );

        // 2. Gọi API Gemini
        String response = gemini.prompt(finalPrompt, apiGenerateUrl);

        // 3. Xử lý Parse JSON
        try {
            // Làm sạch chuỗi JSON (xóa các dấu ```json nếu có)
            String cleanJson = response.replaceAll("(?s)```json(.*?)```|```", "$1").trim();

            // Parse trực tiếp sang Object DTO
            return objectMapper.readValue(cleanJson, CLOsGenerationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response: {}", response);
            // Ném lỗi hệ thống nếu AI trả về sai định dạng
            throw new AppException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    /**
     * Dùng AI để Check ra CLO
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    public CloCheckResponse checkClo(CloCheckRequest req, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!"HOPDC".equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Tạo Prompt từ Template
        // Trong GeminiService.java
        String prompt = String.format(promptTemplateService.get(PromptKey.VALIDATOR_PROMPT),
                req.getTargetLevel(),  // (1)
                req.getTargetLevel(),  // (2)
                req.getCloName(),      // (3)
                req.getCloContent(),   // (4)
                req.getTargetLevel(),  // (5)
                req.getTargetLevel(),  // (6)
                req.getTargetLevel()   // (7)
        );

        // 2. Gọi API thông qua Gemini Client
        String jsonResult = gemini.prompt(prompt, apiCheckUrl);

        // 3. Parse chuỗi JSON thành Object Java
        try {
            // Làm sạch chuỗi JSON (xử lý khối code markdown nếu có)
            String cleanJson = jsonResult.replaceAll("(?s)```json(.*?)```|```", "$1").trim();

            // Sử dụng objectMapper chung của class để parse
            return objectMapper.readValue(cleanJson, CloCheckResponse.class);

        } catch (JsonProcessingException e) {
            log.error("AI Validation Response Error: {}", jsonResult);
            // Ném lỗi 9002 (Invalid Format) mà chúng ta đã thêm trước đó
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        } catch (Exception e) {
            log.error("Unexpected error during CLO validation: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Xóa dấu ngoặc kép, xóa chữ Output thừa, xóa khoảng trắng đầu cuối
        return text.replace("\"", "")
                .replace("Output:", "")
                .replace("*", "") // Gemini hay trả về dấu * đầu dòng
                .trim();
    }

    /**
     * Phân ra các khối block bằng model-embedding
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    @Transactional
    public List<Double> getEmbeddingVector(String text) {
        try {
            List<Double> vector = gemini.getEmbedding(text, apiEmbeddingUrl);
            return vector;
        } catch (Exception e) {
            throw new AppException(ErrorCode.EMBEDDING_FAILED);
        }
    }

    /**
     * So sánh điểm khác biệt giữa 2 syllabus
     */
    @Transactional
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    public ComparisonResult compareSyllabus(SyllabusStructureResponse oldStruct, SyllabusStructureResponse newStruct) {
        try {
            // 2. Convert sang JSON String để nhét vào Prompt
            String oldJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(oldStruct);
            String newJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newStruct);

            // 3. Tạo Prompt
            String prompt = String.format(promptTemplateService.get(PromptKey.COMPARISON_PROMPT),
                    oldJson,
                    newJson);

            // 4. Gọi Gemini API
            String rawResponse = gemini.prompt(prompt, apiGenerateUrl);

            // 5. Clean & Parse kết quả
            String cleanJson = cleanJsonBlock(rawResponse);
            return objectMapper.readValue(cleanJson, ComparisonResult.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze syllabus differences");
        }
    }

    /**
     * Phân tích sự ảnh hưởng từ nội dung bị lược bỏ với môn ảnh hưởng
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    public String determineImapact(String gapConcept, String contextText){
        String prompt = String.format(promptTemplateService.get(PromptKey.DETERMINE_IMPACT),
                gapConcept,
                contextText);
        return gemini.prompt(prompt, apiGenerateUrl);
    }

    /**
     * Upload file PDF lên Google Cloud và lấy file_uri
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // Thử lại sau 2 giây, tối đa 3 lần
    )
    public ProgramRegulationResponse extractMasterDataFromPdf(MultipartFile file, String accountId) {
        // 0. Phân quyền (Tùy chọn theo logic của bạn)
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        if (!"HOCFDC".equals(roleName)) { // Hoặc role Admin
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // 1. Upload File lên Google Cloud lấy file_uri
        String fileUri = gemini.uploadFile(file, apiUploadFileUrl); // gemini chính là GeminiConfig
        if (fileUri == null) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED); // Tạo thêm ErrorCode tương ứng
        }

        // 2. Chuẩn bị Prompt và gọi AI
        String finalPrompt = promptTemplateService.get(PromptKey.MASTER_DATA_EXTRACTOR_PROMPT);
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/pdf";

        // Gọi hàm prompt hỗ trợ file
        String response = gemini.promptWithFile(finalPrompt, fileUri, mimeType, apiGenerateUrl);

        // Nếu API trả về chuỗi báo lỗi do Exception catch ở config
        if (response.startsWith("Lỗi gọi") || response.startsWith("Không có phản hồi")) {
            log.error("AI Generation failed. Message: {}", response);
            throw new AppException(ErrorCode.AI_GENERATION_FAILED);
        }

        // 3. Xử lý Parse JSON
        try {
            // Làm sạch chuỗi JSON (xóa các dấu ```json nếu có)
            String cleanJson = response.replaceAll("(?s)```json(.*?)```|```", "$1").trim();

            // Parse trực tiếp sang Object DTO (Ví dụ class chứa danh sách Môn học, Tín chỉ, PLOs)
            return objectMapper.readValue(cleanJson, ProgramRegulationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response for Master Data: {}", response);
            throw new AppException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String cleanJsonBlock(String response) {
        if (response.contains("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        return response;
    }
}
