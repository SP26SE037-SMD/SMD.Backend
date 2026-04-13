package com.example.smd.services;

import com.example.smd.dto.excel.PrerequisiteImportDTO;
import com.example.smd.dto.request.PrerequisiteRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.prerequisite.ImportPrerequisiteResponse;
import com.example.smd.dto.response.prerequisite.ImportPrerequisiteResult;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Subject_Prerequisite;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PrerequisiteMapper;
import com.example.smd.repositories.PrerequisiteRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.services.excelService.ExcelImporter;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrerequisiteService {

    PrerequisiteRepository prerequisiteRepository;
    SubjectRepository subjectRepository;
    PrerequisiteMapper prerequisiteMapper;

    @Transactional
    public PrerequisiteResponse create(PrerequisiteRequest request) {
        UUID sId = UUID.fromString(request.getSubjectId());
        UUID pId = UUID.fromString(request.getPrerequisiteSubjectId());

        if (sId.equals(pId))
            throw new AppException(ErrorCode.PREREQUISITE_SELF_REFERENCE);
        if (prerequisiteRepository.existsBySubject_SubjectIdAndPrerequisiteSubject_SubjectId(sId, pId))
            throw new AppException(ErrorCode.PREREQUISITE_ALREADY_EXISTS);

        Subject subject = subjectRepository.findById(sId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        Subject preSubject = subjectRepository.findById(pId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        Subject_Prerequisite entity = Subject_Prerequisite.builder()
                .subject(subject)
                .prerequisiteSubject(preSubject)
                .isMandatory(request.getIsMandatory())
                .build();

        return prerequisiteMapper.toResponse(prerequisiteRepository.save(entity));
    }

    @Transactional
    public List<PrerequisiteResponse> getDependents(String subjectId) {
        // Tìm các môn học phụ thuộc vào môn này (Môn này là tiên quyết của chúng)
        List<PrerequisiteResponse> flatList = new ArrayList<>();
        Set<UUID> visitedSubjectIds = new HashSet<>();

        // Queue để duyệt các môn học phụ thuộc theo từng cấp
        Queue<UUID> queue = new LinkedList<>();

        UUID startId = UUID.fromString(subjectId);
        queue.add(startId);
        visitedSubjectIds.add(startId);

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();

            // Tìm các môn mà 'currentId' đang đóng vai trò là môn TIÊN QUYẾT của chúng
            List<Subject_Prerequisite> dependents = prerequisiteRepository
                    .findByPrerequisiteSubject_SubjectId(currentId);

            for (Subject_Prerequisite entity : dependents) {
                // Lấy môn học phụ thuộc (môn đứng sau)
                UUID dependentId = entity.getSubject().getSubjectId();

                // Nếu môn này chưa được duyệt (tránh vòng lặp vô tận)
                if (!visitedSubjectIds.contains(dependentId)) {
                    // 1. Thêm vào danh sách phẳng trả về
                    flatList.add(prerequisiteMapper.toResponse(entity));

                    // 2. Đánh dấu đã xem và đưa vào hàng đợi để tìm tiếp các môn phụ thuộc của nó
                    visitedSubjectIds.add(dependentId);
                    queue.add(dependentId);
                }
            }
        }

        return flatList;
    }

    @Transactional
    public List<PrerequisiteResponse> getPrerequisites(String subjectId) {
        List<PrerequisiteResponse> flatList = new ArrayList<>();
        Set<UUID> visitedSubjectIds = new HashSet<>();

        // Bắt đầu khử đệ quy bằng Queue (BFS)
        Queue<UUID> queue = new LinkedList<>();
        queue.add(UUID.fromString(subjectId));
        visitedSubjectIds.add(UUID.fromString(subjectId));

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();

            // Tìm các môn tiên quyết trực tiếp của môn hiện tại
            List<Subject_Prerequisite> dependencies = prerequisiteRepository.findBySubject_SubjectId(currentId);

            for (Subject_Prerequisite entity : dependencies) {
                UUID preId = entity.getPrerequisiteSubject().getSubjectId();

                // Nếu môn tiên quyết này chưa được xử lý
                if (!visitedSubjectIds.contains(preId)) {
                    // 1. Map sang Response và add vào list phẳng
                    flatList.add(prerequisiteMapper.toResponse(entity));

                    // 2. Đánh dấu đã xem và cho vào queue để tìm tiếp "ông nội" của nó
                    visitedSubjectIds.add(preId);
                    queue.add(preId);
                }
            }
        }

        return flatList;
    }

    @Transactional
    public List<PrerequisiteResponse> getDependentsByCode(String subjectCode) {
        Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        return getDependents(subject.getSubjectId().toString());
    }

    @Transactional
    public List<PrerequisiteResponse> getPrerequisitesByCode(String subjectCode) {
        Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        return getPrerequisites(subject.getSubjectId().toString());
    }

    public void delete(UUID id) {
        if (!prerequisiteRepository.existsById(id))
            throw new AppException(ErrorCode.PREREQUISITE_NOT_FOUND);

        prerequisiteRepository.deleteById(id);
    }

    @Transactional
    public ImportPrerequisiteResponse importPrerequisites(MultipartFile file) {
        List<ImportPrerequisiteResult> details = new ArrayList<>();
        List<Subject_Prerequisite> entitiesToSave = new ArrayList<>();
        Set<String> uniquePairsInFile = new HashSet<>();

        try {
            List<PrerequisiteImportDTO> rows = ExcelImporter.importFromExcel(file, PrerequisiteImportDTO.class);

            for (PrerequisiteImportDTO row : rows) {
                String subjectCode = trim(row.getSubjectCode());
                String prerequisiteCode = trim(row.getPrerequisiteSubjectCode());

                if (subjectCode == null || prerequisiteCode == null) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("Missing required fields: Subject code, Subject Prerequisite")
                            .build());
                    continue;
                }

                Subject subject = subjectRepository.findBySubjectCode(subjectCode).orElse(null);
                if (subject == null) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("Subject code not found")
                            .build());
                    continue;
                }

                Subject prerequisiteSubject = subjectRepository.findBySubjectCode(prerequisiteCode).orElse(null);
                if (prerequisiteSubject == null) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("Prerequisite subject code not found")
                            .build());
                    continue;
                }

                if (subject.getSubjectId().equals(prerequisiteSubject.getSubjectId())) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("A subject cannot be its own prerequisite")
                            .build());
                    continue;
                }

                String uniquePair = subject.getSubjectId() + "-" + prerequisiteSubject.getSubjectId();
                if (!uniquePairsInFile.add(uniquePair)) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("Duplicate prerequisite mapping in file")
                            .build());
                    continue;
                }

                if (prerequisiteRepository.existsBySubject_SubjectIdAndPrerequisiteSubject_SubjectId(
                        subject.getSubjectId(), prerequisiteSubject.getSubjectId())) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message("This prerequisite relationship already exists")
                            .build());
                    continue;
                }

                Boolean isMandatory;
                try {
                    isMandatory = parseMandatory(row.getIsMandatory());
                } catch (AppException ex) {
                    details.add(ImportPrerequisiteResult.builder()
                            .subjectCode(subjectCode)
                            .prerequisiteSubjectCode(prerequisiteCode)
                            .status("FAILED")
                            .message(ex.getMessage())
                            .build());
                    continue;
                }

                entitiesToSave.add(Subject_Prerequisite.builder()
                        .subject(subject)
                        .prerequisiteSubject(prerequisiteSubject)
                        .isMandatory(isMandatory)
                        .build());

                details.add(ImportPrerequisiteResult.builder()
                        .subjectCode(subjectCode)
                        .prerequisiteSubjectCode(prerequisiteCode)
                        .status("SUCCESS")
                        .message("Created successfully")
                        .build());
            }

            prerequisiteRepository.saveAll(entitiesToSave);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import prerequisite failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportPrerequisiteResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .details(details)
                .build();
    }

    private Boolean parseMandatory(String raw) {
        String value = trim(raw);
        if (value == null) {
            return true;
        }

        String normalized = value.toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "y".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "n".equals(normalized)) {
            return false;
        }

        throw new AppException(ErrorCode.INVALID_KEY, "Invalid isMandantory value: " + value);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
