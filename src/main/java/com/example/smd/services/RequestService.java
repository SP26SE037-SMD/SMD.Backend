package com.example.smd.services;

import com.example.smd.dto.request.request.RequestCreateRequest;
import com.example.smd.dto.request.request.RequestUpdateRequest;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.entities.Request;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.RequestMapper;
import com.example.smd.repositories.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestService {

    RequestRepository requestRepository;
    AccountRepository accountRepository;
    RequestMapper requestMapper;

    // Repositories for target enrichment
    SubjectRepository    subjectRepository;
    SyllabusRepository   syllabusRepository;
    CurriculumRepository curriculumRepository;
    MajorRepository      majorRepository;
    TaskV2Repository     taskV2Repository;

    // ------------------------------------------------------------------ CREATE

    @Transactional
    public RequestResponse create(RequestCreateRequest dto, String createdByUserId) {
        Request request = requestMapper.toEntity(dto);

        // createdBy từ JWT
        request.setCreatedBy(
                accountRepository.findById(UUID.fromString(createdByUserId))
                        .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));

        // receivedBy (tuỳ chọn)
        if (dto.getReceivedById() != null) {
            request.setReceivedBy(
                    accountRepository.findById(dto.getReceivedById())
                            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        request.setStatus("PENDING");

        return enrichFromEntity(requestRepository.save(request));
    }

    // ------------------------------------------------------------------ READ

    @Transactional(readOnly = true)
    public Page<RequestResponse> getAll(
            String search,
            String status,
            String type,
            UUID createdById,
            UUID receivedById,
            UUID targetId,
            Pageable pageable) {

        var spec = RequestSpecification.withFilters(search, status, type, createdById, receivedById, targetId);
        return requestRepository.findAll(spec, pageable)
                .map(this::enrichFromEntity);
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(UUID id) {
        return enrichFromEntity(
                requestRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND)));
    }

    // ------------------------------------------------------------------ UPDATE STATUS

    /**
     * Chỉ người nhận (receivedBy) mới được phép cập nhật trạng thái + comment.
     * Các trường khác (title, content, type, targetId) không được thay đổi ở đây.
     */
    @Transactional
    public RequestResponse updateStatus(UUID id, RequestUpdateRequest dto) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        requestMapper.updateEntity(request, dto);

        if (dto.getReceivedById() != null) {
            request.setReceivedBy(
                    accountRepository.findById(dto.getReceivedById())
                            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        }

        return enrichFromEntity(requestRepository.save(request));
    }

    // ------------------------------------------------------------------ DELETE

    @Transactional
    public void delete(UUID id) {
        if (!requestRepository.existsById(id)) {
            throw new AppException(ErrorCode.REQUEST_NOT_FOUND);
        }
        requestRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ ENRICH

    /**
     * Map a Request entity to RequestResponse, then resolve targetId
     * into a rich nested DTO based on the request type.
     * This must be called within an active transaction so that
     * lazy-loaded relations (e.g. department, subject) are accessible.
     *
     * SUBJECT    -> SubjectDto
     * SYLLABUS   -> SyllabusDto (+ parent subject info)
     * CURRICULUM -> CurriculumDto
     * MAJOR      -> MajorDto
     * TASK       -> TaskDto
     */
    private RequestResponse enrichFromEntity(Request entity) {
        RequestResponse response = requestMapper.toResponse(entity);

        if (entity.getTargetId() == null || entity.getType() == null) {
            return response;
        }

        UUID targetId = entity.getTargetId();

        switch (entity.getType().toUpperCase()) {
            case "SUBJECT":
                subjectRepository.findById(targetId).ifPresent(sub -> {
                    String deptCode = sub.getDepartment() != null ? sub.getDepartment().getDepartmentCode() : null;
                    String deptName = sub.getDepartment() != null ? sub.getDepartment().getDepartmentName() : null;
                    response.setSubject(RequestResponse.SubjectDto.builder()
                            .subjectId(sub.getSubjectId())
                            .subjectCode(sub.getSubjectCode())
                            .subjectName(sub.getSubjectName())
                            .credits(sub.getCredits())
                            .status(sub.getStatus())
                            .departmentCode(deptCode)
                            .departmentName(deptName)
                            .build());
                });
                break;

            case "SYLLABUS":
                syllabusRepository.findById(targetId).ifPresent(s -> {
                    RequestResponse.SyllabusDto.SyllabusDtoBuilder builder = RequestResponse.SyllabusDto.builder()
                            .syllabusId(s.getSyllabusId())
                            .syllabusName(s.getSyllabusName())
                            .status(s.getStatus());
                    if (s.getSubject() != null) {
                        builder.subjectId(s.getSubject().getSubjectId())
                               .subjectCode(s.getSubject().getSubjectCode())
                               .subjectName(s.getSubject().getSubjectName());
                    }
                    response.setSyllabus(builder.build());
                });
                break;

            case "CURRICULUM":
                curriculumRepository.findById(targetId).ifPresent(c ->
                        response.setCurriculum(RequestResponse.CurriculumDto.builder()
                                .curriculumId(c.getCurriculumId())
                                .curriculumCode(c.getCurriculumCode())
                                .curriculumName(c.getCurriculumName())
                                .build()));
                break;

            case "MAJOR":
                majorRepository.findById(targetId).ifPresent(m ->
                        response.setMajor(RequestResponse.MajorDto.builder()
                                .majorId(m.getMajorId())
                                .majorCode(m.getMajorCode())
                                .majorName(m.getMajorName())
                                .status(m.getStatus())
                                .build()));
                break;

            case "TASK":
                taskV2Repository.findById(targetId).ifPresent(t ->
                        response.setTask(RequestResponse.TaskDto.builder()
                                .taskId(t.getTaskId())
                                .taskName(t.getTaskName())
                                .status(t.getStatus())
                                .type(t.getType())
                                .action(t.getAction())
                                .build()));
                break;

            default:
                break;
        }

        return response;
    }
}
