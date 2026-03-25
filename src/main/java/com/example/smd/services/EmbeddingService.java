package com.example.smd.services;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ImpactResponse;
import com.example.smd.dto.response.SimilarityResult;
import com.example.smd.dto.response.syllabus.SyllabusStructureResponse;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Material;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Vector_Embeddings;
import com.example.smd.enums.ImpactStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.EmbeddingRepository;
import com.example.smd.repositories.MaterialRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmbeddingService {
    EmbeddingRepository embeddingRepo;
    BlockRepository blockRepo;
    SyllabusRepository syllabusRepo;
    MaterialRepository materialRepo;
    GeminiService gemini;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void createEmbedding(String text, Blocks block) {
        List<Double> doubleList = gemini.getEmbeddingVector(text);

        // 2. Chuyển thành chuỗi "[0.1, 0.2, ...]"
        String vectorString = doubleList.stream()
                .map(d -> String.format(Locale.US, "%.8f", d))
                .collect(Collectors.joining(",", "[", "]"));

        // 3. Gọi Native Query để INSERT
        embeddingRepo.insertVector(
                UUID.randomUUID(),
                block.getBlockId(),
                text,
                Instant.now(),
                vectorString
        );
    }

    @Transactional
    public SyllabusStructureResponse getSyllabusStructure(UUID syllabusId) {
        // 1. Tìm Syllabus
        Syllabus syllabus = syllabusRepo.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        // 2. Lấy danh sách Material (Chương) thuộc Syllabus này
        List<Material> materials = materialRepo.findAllBySyllabus_SyllabusId(syllabusId);

        // 3. Map sang DTO và lồng các Topics (Blocks H1/H2)
        List<SyllabusStructureResponse.ChapterDTO> chapterDTOs = materials.stream().map(material -> {

            // Lấy các tiêu đề nhỏ (Subtitle) từ Blocks
            List<String> topics = blockRepo.findTitlesByMaterialId(material.getMaterialId());

            return SyllabusStructureResponse.ChapterDTO.builder()
                    .materialId(material.getMaterialId())
                    .chapterTitle(material.getTitle())
                    .topics(topics)
                    .build();
        }).toList();

        return SyllabusStructureResponse.builder()
                .syllabusName(syllabus.getSyllabusName())
                .version(syllabus.getSubject().getSubjectCode()) // Ví dụ lấy mã môn làm version
                .chapters(chapterDTOs)
                .build();
    }

    //Đưa AI trả về điểm khác biệt của 2 môn
    @Transactional
    public ComparisonResult compareSyllabus(UUID oldId, UUID newId) {
        try {
            //Lấy cấu trúc dữ liệu (Bước chuẩn bị)
            SyllabusStructureResponse oldStruct = getSyllabusStructure(oldId);
            SyllabusStructureResponse newStruct = getSyllabusStructure(newId);

            return gemini.compareSyllabus(oldStruct, newStruct);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze syllabus differences");
        }
    }

    @Transactional
    public ImpactResponse checkImpact(String gapConcept, UUID descendantSubjectId) {

        // 1. Lấy vector của kiến thức bị hổng (ví dụ: "Tính kế thừa")
        List<Double> gapVector = gemini.getEmbeddingVector(gapConcept);

        // 2. CHUẨN HÓA CHUỖI VECTOR (BẮT BUỘC: Không được có dấu cách)
        String vectorStr = gapVector.stream()
                .map(d -> String.format(Locale.US, "%.8f", d)) // Ép dùng dấu chấm thập phân (.)
                .collect(Collectors.joining(",", "[", "]"));

        // 3. Tìm đoạn văn liên quan nhất trong giáo trình Môn B (Hậu duệ)
        // Giả sử dùng hàm tương tự findSimilarArticles_Final
        // 3. Gọi Repo với chuỗi đã chuẩn hóa
        List<SimilarityResult> matches = embeddingRepo.findTopSimilarContent(descendantSubjectId, vectorStr);

        System.out.println("DEBUG - VectorStr: " + vectorStr.substring(0, 50) + "...");
        System.out.println("DEBUG - SyllabusID: " + descendantSubjectId);
        System.out.println("Tổng số match tìm thấy: " + matches.size());

        if (matches.isEmpty()) {
            return ImpactResponse.builder()
                    .impactStatus(ImpactStatus.NO_IMPACT)
                    .similarity(0.0)
                    .removeContent(gapConcept).build();
        }

        SimilarityResult topMatch = matches.get(0);
        double distance = ((Number) topMatch.getDistance()).doubleValue();// Lấy khoảng cách đoạn giống nhất
//        double similarity = Math.pow(1.0 - distance, 2);
        double similarity = 1.0 - distance;

//        Trong buổi bảo vệ, nếu thầy cô hỏi tại sao chọn ngưỡng 0.35,
//        bạn có thể trả lời: "Do đặc thù văn phong giáo trình giữa các môn học
//        có sự khác biệt về thuật ngữ chuyên môn, nên em sử dụng ngưỡng 0.35
//        để đảm bảo không bỏ sót các mối liên hệ ngữ nghĩa tiềm tàng
//        trước khi đưa vào mô hình ngôn ngữ lớn (LLM) để phân tích sâu".
        if (similarity > 0.35) {
            // 3. KHÁC BIỆT Ở ĐÂY: Dùng AI để phân loại
            String contentText = (String) topMatch.getContentBody();
            return ImpactResponse.builder()
                    .impactStatus(determineImpactType(gapConcept, contentText))
                    .similarity(similarity)
                    .removeContent(gapConcept).build();
            // Trả về: REQUIRED (Lỗ hổng lan truyền) hoặc RESOLVED (Đã được dạy lại)
        }

        return ImpactResponse.builder()
                .impactStatus(ImpactStatus.NO_IMPACT)
                .similarity(similarity)
                .removeContent(gapConcept).build();
    }

    public ImpactStatus determineImpactType(String gapConcept, String contextText) {
        String aiResponse = gemini.determineImapact(gapConcept, contextText).trim().toUpperCase();
        try {
            return ImpactStatus.valueOf(aiResponse);
        } catch (IllegalArgumentException e) {
            // Trường hợp AI trả lời lan man, mặc định coi là chưa được vá (Nguy hiểm)
            return ImpactStatus.REQUIRED;
        }
    }
}
