package com.example.smd.services;

import com.example.smd.dto.response.*;
import com.example.smd.dto.response.syllabus.SyllabusStructureResponse;
import com.example.smd.dto.response.validate.CompareSyllabusResponse;
import com.example.smd.entities.*;
import com.example.smd.enums.RoleName;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmbeddingService {
    BlockRepository blockRepo;
    SyllabusRepository syllabusRepo;
    MaterialRepository materialRepo;
    AssessmentRepository assessmentRepo;
    GeminiService gemini;
    SyllabusComparisonHistoryRepository historyRepo;
    AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // createEmbedding removed: vector_embeddings table has been dropped

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
            log.info("Topic material {}: {}", material.getMaterialId(), topics);
            return SyllabusStructureResponse.ChapterDTO.builder()
                    .materialId(material.getMaterialId())
                    .chapterTitle(material.getTitle())
                    .topics(topics)
                    .build();
        }).toList();

        return SyllabusStructureResponse.builder()
                .syllabusName(syllabus.getSyllabusName())
                .version(syllabus.getSubject().getSubjectCode())
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

    public AssessmentDiffResponse compareAssessmentConfiguration(UUID oldId, UUID newId) {

        List<Assessment> oldList = assessmentRepo.findBySyllabus_SyllabusId(oldId);
        List<Assessment> newList = assessmentRepo.findBySyllabus_SyllabusId(newId);
        AssessmentDiffResponse diff = new AssessmentDiffResponse();

        java.util.function.Function<Assessment, String> getIdentifier = a -> {
            String typeName = (a.getAssessmentType() != null)
                    ? a.getAssessmentType().getTypeName()
                    : "Unknown Type";

            Integer part = (a.getPart() != null) ? a.getPart() : 1;

            return String.format("%s - Part %d", typeName, part);
        };

        Map<String, Assessment> oldMap = oldList.stream()
                .collect(Collectors.toMap(getIdentifier, a -> a, (existing, replacement) -> existing));
        Map<String, Assessment> newMap = newList.stream()
                .collect(Collectors.toMap(getIdentifier, a -> a, (existing, replacement) -> existing));

        for (Assessment oldItem : oldList) {
            String identifier = getIdentifier.apply(oldItem);

            if (!newMap.containsKey(identifier)) {
                diff.getRemovedAssessments().add(identifier);
            } else {
                Assessment newItem = newMap.get(identifier);
                List<String> changes = new ArrayList<>();

                // Check Loại bài thi (ví dụ: Summative / Formative)
                if (oldItem.getAssessmentType() != null && newItem.getAssessmentType() != null) {
                    if (!oldItem.getAssessmentType().getTypeId().equals(newItem.getAssessmentType().getTypeId())) {
                        changes.add(String.format("The rating type changes from '%s' to '%s'",
                                oldItem.getAssessmentType().getTypeName(), newItem.getAssessmentType().getTypeName()));
                    }
                }

                // Check Trọng số điểm (Kiểu dữ liệu Double - Cần chống sai lệch số thập phân)
                if (oldItem.getWeight() != null && newItem.getWeight() != null) {
                    if (Double.compare(oldItem.getWeight(), newItem.getWeight()) != 0) {
                        changes.add(String.format("The weight changes from %.1f%% to %.1f%%", oldItem.getWeight(), newItem.getWeight()));
                    }
                }

                // Check Thời gian làm bài (Duration)
                if (!java.util.Objects.equals(oldItem.getDuration(), newItem.getDuration())) {
                    changes.add(String.format("The time allotted for the exam has changed from %d minutes to %d minutes.",
                            oldItem.getDuration() != null ? oldItem.getDuration() : 0,
                            newItem.getDuration() != null ? newItem.getDuration() : 0));
                }

                // Check Tiêu chí hoàn thành (Completion Criteria)
                if (!java.util.Objects.equals(oldItem.getCompletionCriteria(), newItem.getCompletionCriteria())) {
                    changes.add(String.format("The condition completes from '%s' to '%s'.",
                            oldItem.getCompletionCriteria(), newItem.getCompletionCriteria()));
                }

                // Check Dạng câu hỏi (Question Type)
                if (!java.util.Objects.equals(oldItem.getQuestionType(), newItem.getQuestionType())) {
                    changes.add(String.format("The question format changes from '%s' to '%s'",
                            oldItem.getQuestionType(), newItem.getQuestionType()));
                }

                // Check Chuẩn kiến thức kỹ năng (Knowledge Skill)
                if (!java.util.Objects.equals(oldItem.getKnowledgeSkill(), newItem.getKnowledgeSkill())) {
                    changes.add(String.format("The standard for knowledge skills changes from '%s' to '%s'",
                            oldItem.getKnowledgeSkill(), newItem.getKnowledgeSkill()));
                }

                // Check Hướng dẫn chấm điểm (Grading Guide)
                if (!java.util.Objects.equals(oldItem.getGradingGuide(), newItem.getGradingGuide())) {
                    changes.add(String.format("Scoring guidelines updated from '%s' to '%s'",
                            oldItem.getGradingGuide(), newItem.getGradingGuide()));
                }

                // Check Ghi chú (Note)
                if (!java.util.Objects.equals(oldItem.getNote(), newItem.getNote())) {
                    changes.add(String.format("The note changes from '%s' to '%s'",
                            oldItem.getNote(), newItem.getNote()));
                }

                // Nếu phát hiện ra có sự lệch thông số cấu hình -> Add vào mảng Thay Đổi
                if (!changes.isEmpty()) {
                    diff.getChangedAssessments().add(new AssessmentDiffResponse.AssessmentChangeDTO(identifier, changes));
                }
            }
        }

        // 4. Quét danh sách mới: Tìm các bài kiểm tra được THÊM MỚI hoàn toàn
        for (Assessment newItem : newList) {
            String identifier = getIdentifier.apply(newItem);
            if (!oldMap.containsKey(identifier)) {
                diff.getAddedAssessments().add(identifier);
            }
        }

        return diff;
    }

    // checkImpact and determineImpactType removed: vector_embeddings table has been dropped

    public void saveComparisonHistory(UUID oldId, UUID newId, AssessmentDiffResponse assessmentResult, ComparisonResult analysis) {
        try {
            String assessmentJsonStr = objectMapper.writeValueAsString(assessmentResult);
            String conceptJsonStr = objectMapper.writeValueAsString(analysis);

            // 2. Build thực thể History
            SyllabusComparisonHistory history = SyllabusComparisonHistory.builder()
                    .oldSyllabusId(oldId)
                    .newSyllabusId(newId)
                    .assessmentDiffJson(assessmentJsonStr)
                    .conceptDiffJson(conceptJsonStr)
                    .selectedCompare(false)
                    .build();

            // 3. Khóa sổ ghi xuống Database
            historyRepo.save(history);

        } catch (JsonProcessingException e) {
            log.error("Lỗi parse cấu trúc dữ liệu sang JSON để lưu lịch sử", e);
            throw new RuntimeException("Save history failed");
        }
    }

    public List<SyllabusComparisonHistory> getComparisonHistoryDetailForHoPDC(UUID newSyllabusId) {
        return historyRepo.findByNewSyllabusIdOrderByCreatedAtDesc(newSyllabusId);
    }

    public SyllabusComparisonHistory getComparisonHistoryDetailForStudent(UUID newSyllabusId) {
        return historyRepo.findFirstByNewSyllabusIdAndSelectedCompareTrueOrderByCreatedAtDesc(newSyllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_HISTORY_NOT_FOUND));
    }

    public boolean validateLatestAndSubsequentVersions(UUID oldId, UUID newId) {

        Syllabus newSyllabus = syllabusRepo.findById(newId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        UUID subjectId = newSyllabus.getSubject().getSubjectId();

        List<Syllabus> versionChain = syllabusRepo.findBySubject_SubjectIdOrderByCreatedAtDesc(subjectId);

        // PHÒNG THỦ: Chuỗi phiên bản tối thiểu phải có 2 bản ghi để cấu thành cặp đối chiếu
        if (versionChain.size() < 2) {
            throw new AppException(ErrorCode.SUBJECT_NOT_HAVE_TWO_SYLLABUS);
        }

        // 4. Tiến hành kiểm tra thứ hạng (Xác định quán quân và á quân)
        UUID absoluteNewestId = versionChain.get(0).getSyllabusId();       // Vị trí 0: Mới nhất tuyệt đối
        UUID absoluteSecondNewestId = versionChain.get(1).getSyllabusId(); // Vị trí 1: Cận cuối

        boolean isNewestValid = newId.equals(absoluteNewestId);
        boolean isOldValid = oldId.equals(absoluteSecondNewestId);

        // 5. Nếu truyền sai thứ tự hoặc cố tình so sánh các bản ghi cũ rích -> Chặn đứng ngay lập tức!
        return (isNewestValid && isOldValid) ? true : false;
    }

    @Transactional
    public CompareSyllabusResponse compareTwoVersionSyllabus(UUID oldSyllabusId, UUID newSyllabusId) {
        AssessmentDiffResponse assessmentResult = compareAssessmentConfiguration(oldSyllabusId, newSyllabusId);
        ComparisonResult analysis = compareSyllabus(oldSyllabusId, newSyllabusId);
        return new CompareSyllabusResponse(oldSyllabusId, newSyllabusId, assessmentResult, analysis);

    }

    public SyllabusComparisonHistory selectHistoryCompare(UUID historyId){
        var history = historyRepo.findById(historyId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_HISTORY_NOT_FOUND));

        var historyList = historyRepo.findByOldSyllabusIdAndNewSyllabusIdOrderByCreatedAtDesc(history.getOldSyllabusId(), history.getNewSyllabusId());
        for (int i = 0; i < historyList.size(); i++) {
            if(historyList.get(i).isSelectedCompare()){
                history.setSelectedCompare(false);
                break;
            }
        }
        history.setSelectedCompare(true);
        return historyRepo.save(history);
    }
}
