package com.example.smd.services;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.syllabus.SyllabusStructureResponse;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Material;
import com.example.smd.entities.Syllabus;
import com.example.smd.entities.Vector_Embeddings;
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
                .map(String::valueOf)
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
}
