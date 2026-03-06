package com.example.smd.services;

import com.example.smd.dto.request.PrerequisiteRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Subject_Prerequisite;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PrerequisiteMapper;
import com.example.smd.repositories.PrerequisiteRepository;
import com.example.smd.repositories.SubjectRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

        if (sId.equals(pId)) throw new AppException(ErrorCode.PREREQUISITE_SELF_REFERENCE);
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
        return prerequisiteRepository.findByPrerequisiteSubject_SubjectId(UUID.fromString(subjectId))
                .stream().map(prerequisiteMapper::toResponse).toList();
    }

    @Transactional
    public List<PrerequisiteResponse> getPrerequisites(String subjectId) {
        // Tìm các bản ghi mà 'subjectId' này đóng vai trò là môn chính (môn sau)
        // Kết quả sẽ trả về các môn học trước (prerequisite_subject_id)
        return prerequisiteRepository.findBySubject_SubjectId(UUID.fromString(subjectId))
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
    }

    public void delete(String id) {
        if (!prerequisiteRepository.existsById(id))
            throw new AppException(ErrorCode.PREREQUISITE_NOT_FOUND);
        prerequisiteRepository.deleteById(id);
    }
}
