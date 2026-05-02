package com.example.smd.services;

import com.example.smd.dto.request.session.SessionMaterialBlockBulkRequest;
import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.entities.Assessment;
import com.example.smd.entities.Session;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SessionMapper;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String SOFT_DELETE_STATUS = "ARCHIVED";

    private final AccountService accountService;
    private final SessionRepository sessionRepository;
    private final SyllabusRepository syllabusRepository;
    private final SessionMapper sessionMapper;
    private final SessionRegulationValidationService sessionRegulationValidationService;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public Page<SessionResponse> getAllSessions(UUID syllabusId,
                                                String status,
                                                String search,
                                                int page,
                                                int size,
                                                String[] sort,
                                                String accountId) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] parsedSort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(parsedSort[1]), parsedSort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!status.equals(MaterialStatus.PUBLISHED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(status)) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        Specification<Session> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (syllabusId != null) {
                predicates.add(cb.equal(root.get("syllabus").get("syllabusId"), syllabusId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("sessionTitle"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("content"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("teachingMethods"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sessionRepository.findAll(specification, pagingSort)
                .map(sessionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(UUID sessionId, String accountId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // 3. Logic Phân quyền:
        // Nếu là STUDENT hoặc LECTURER, chỉ cho phép xem nếu status là PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!MaterialStatus.PUBLISHED.toString().equalsIgnoreCase(session.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if ("DRAFT".equals(session.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(session.getStatus())) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return sessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsBySyllabus(UUID syllabusId) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        return sessionRepository.findBySyllabus_SyllabusIdOrderBySessionNumberAsc(syllabusId)
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
            throw new AppException(ErrorCode.SESSION_CANNOT_CREATE);
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumber(
                request.getSyllabusId(), request.getSessionNumber())) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

        String newType = "";
        if (SessionType.THEORY.toString().equals(request.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        Session session = sessionMapper.toEntity(request);
        session.setSessionType(newType);
        session.setSyllabus(syllabus);
        session.setStatus(DEFAULT_STATUS);

        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionRequest request, String accountId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!("DRAFT".equals(session.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(session.getStatus()))) {
            throw new AppException(ErrorCode.SESSION_NOT_EDITABLE);
        }

        String newType = "";
        if (SessionType.THEORY.toString().equals(request.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
                request.getSyllabusId(), request.getSessionNumber(), sessionId)) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

//        sessionRegulationValidationService.validateDurationByRegulation(
//            request.getSyllabusId(),
//            request.getDuration(),
//            sessionId
//        );

        session.setSyllabus(syllabus);
        session.setSessionType(newType);
        sessionMapper.updateEntity(session, request);

        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public List<SessionResponse> createSessionsBluk (List<SessionRequest> requests, String accountId) {
        // 1. Kiểm tra quyền (Chỉ cần check 1 lần cho toàn bộ request)
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        List<Session> sessionsToSave = new ArrayList<>();

        // Cache Syllabus lại để tránh gọi DB nhiều lần nếu các session đều thuộc chung 1 Syllabus
        Map<UUID, Syllabus> syllabusCache = new HashMap<>();

        // Dùng Set để track các session_number đang được tạo trong cùng list này để tránh duplicate
        Set<String> sessionNumberTracker = new HashSet<>();

        for (SessionRequest request : requests) {
            // 2. Validate và Cache Syllabus
            Syllabus syllabus = syllabusCache.computeIfAbsent(request.getSyllabusId(), id -> {
                Syllabus s = syllabusRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

                if (!(SyllabusStatus.IN_PROGRESS.toString().equals(s.getStatus()) ||
                        SyllabusStatus.REVISION_REQUESTED.toString().equals(s.getStatus()))) {
                    throw new AppException(ErrorCode.SESSION_CANNOT_CREATE);
                }
                return s;
            });

            // 3. Kiểm tra trùng sessionNumber dưới Database
            if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumber(
                    request.getSyllabusId(), request.getSessionNumber())) {
                throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
            }

            // 4. Kiểm tra trùng sessionNumber ngay trong list request gửi lên
            String trackerKey = request.getSyllabusId() + "_" + request.getSessionNumber();
            if (!sessionNumberTracker.add(trackerKey)) {
                throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
            }

            // 5. Xử lý Session Type
            String newType = "";
            if (SessionType.THEORY.toString().equals(request.getSessionType())) {
                newType = SessionType.THEORY.toString();
            } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
                newType = SessionType.PRACTICE.toString();
            } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
                newType = SessionType.SELF_STUDY.toString();
            }

            // 6. Map dữ liệu
            Session session = sessionMapper.toEntity(request);
            session.setSessionType(newType);
            session.setSyllabus(syllabus);
            session.setStatus(DEFAULT_STATUS); // Chú ý: đảm bảo DEFAULT_STATUS đã được define

            sessionsToSave.add(session);
        }

        // 7. Save tất cả 1 lần xuống DB để tối ưu performance (Batch Insert)
        List<Session> savedSessions = sessionRepository.saveAll(sessionsToSave);

        // 8. Map sang Response và trả về
        return savedSessions.stream()
                .map(sessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionResponse updateSessionStatus(UUID sessionId, String status) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (status == null || status.trim().isEmpty()) {
            throw new AppException(ErrorCode.SESSION_STATUS_REQUIRED);
        }

        session.setStatus(status.trim());
        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public boolean deleteSession(UUID sessionId, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(session.getSyllabus().getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(session.getSyllabus().getStatus()))) {
            throw new AppException(ErrorCode.SESSION_NOT_EDITABLE);
        }

        if (DEFAULT_STATUS.equalsIgnoreCase(session.getStatus())) {
            sessionRepository.delete(session);
            return true;
        }

        session.setStatus(SOFT_DELETE_STATUS);
        sessionRepository.save(session);
        return true;
    }

    @Transactional
    public boolean deleteSessionListBySyllabusAndSessionNumbers(UUID syllabusId, SessionNumberListRequest request) {
        if (request == null || request.getSessionNumbers() == null || request.getSessionNumbers().isEmpty()) {
            throw new AppException(ErrorCode.SESSION_NUMBER_LIST_REQUIRED);
        }

        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        List<Integer> distinctNumbers = request.getSessionNumbers().stream().distinct().toList();
        List<Session> sessions = sessionRepository.findBySyllabus_SyllabusIdAndSessionNumberIn(syllabusId, distinctNumbers);

        if (sessions.size() != distinctNumbers.size()) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        }

        boolean hasNonDraft = sessions.stream()
                .anyMatch(session -> !DEFAULT_STATUS.equalsIgnoreCase(session.getStatus()));
        if (hasNonDraft) {
            throw new AppException(ErrorCode.SESSION_NOT_DRAFT);
        }

        sessionRepository.deleteAll(sessions);
        return true;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        }
        if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    @jakarta.transaction.Transactional
    public void updateSessionStatusBySyllabus(String syllabusId, String newStatus) {
        // 1. Kiểm tra trạng thái hợp lệ từ Enum SyllabusStatus (hoặc MaterialStatus nếu bạn có riêng)
        SyllabusStatus status;
        try {
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_MATERIAL_STATUS);
        }

        UUID uuidSyllabusId = UUID.fromString(syllabusId);

        // 2. Kiểm tra Syllabus có tồn tại không trước khi update Material
        if (!syllabusRepository.existsById(uuidSyllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        // 3. Cập nhật hàng loạt trạng thái các Materials thuộc Syllabus này
        // Lưu ý: Material đi theo Syllabus nên ta dùng updateStatusBySyllabusId
        int affectedRows = sessionRepository.updateStatusBySyllabusId(status.toString(), uuidSyllabusId);
    }

    public SessionValidationResult validate(List<SessionRequest> inputs, UUID syllabusId) {
        SessionValidationResult result = new SessionValidationResult();

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        Subject masterSubject = subjectRepository.findById(syllabus.getSubject().getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // LẤY DỮ LIỆU HIỆN TẠI TỪ DATABASE
        List<Session> existingDbSessions = sessionRepository.findBySyllabus_SyllabusId(syllabusId);
        // 1. Tính quỹ Lý thuyết (Quy đổi an toàn từ Giờ -> Tiết)
        double inputTotalTheoryHours = inputs.stream()
                .filter(s -> "THEORY".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0) // Dùng Double để nhận số lẻ 1.5, 2.25
                .sum();
        double dbTotalTheoryHours = existingDbSessions.stream()
                .filter(s -> "THEORY".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0) // Dùng Double để nhận số lẻ 1.5, 2.25
                .sum();
        int inputTotalTheoryPeriods = (int) Math.round(inputTotalTheoryHours / 45);
        int dbTotalTheoryPeriods = (int) Math.round(dbTotalTheoryHours / 45);
        int remainingTheory = masterSubject.getTheoryPeriods() - inputTotalTheoryPeriods - dbTotalTheoryPeriods;

        // 2. Tính quỹ Thực hành (Tương tự)
        double inputTotalPracticeHours = inputs.stream()
                .filter(s -> "PRACTICE".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        double dbTotalPracticeHours = inputs.stream()
                .filter(s -> "PRACTICE".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        int inputTotalPracticePeriods = (int) Math.round(inputTotalPracticeHours / 45);
        int dbTotalPracticePeriods = (int) Math.round(dbTotalPracticeHours / 45);
        int remainingPractice = masterSubject.getPracticalPeriods() - inputTotalPracticePeriods - dbTotalPracticePeriods;

        // (Tùy chọn) Tính tổng giờ tự học nếu có bắt validate
        int inputTotalSelfStudyHours = inputs.stream()
                .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
                .sum();
        int dbTotalSelfStudyHours = inputs.stream()
                .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
                .sum();
        int remainingSelfStudy = masterSubject.getSelfStudyPeriods() - inputTotalSelfStudyHours - dbTotalSelfStudyHours;

        // Set vào DTO
        result.setRemainingQuotas(new SessionValidationResult.RemainingQuota(remainingTheory, remainingPractice, 0));

        // 2. Viết Logic Check Lỗi

        // -- Validate Lý thuyết (Theory) --
        if (remainingTheory > 0) {
            // Trường hợp THIẾU (Allocated < Quota)
            result.addError("THEORY_SHORTAGE",
                    "Theory allocation is short by " + remainingTheory + " period(s).");
        } else if (remainingTheory < 0) {
            // Trường hợp DƯ (Allocated > Quota)
            result.addError("THEORY_SURPLUS",
                    "Theory allocation exceeded by " + Math.abs(remainingTheory) + " period(s).");
        }

        // -- Validate Thực hành (Practice) --
        if (remainingPractice > 0) {
            // Trường hợp THIẾU
            result.addError("PRACTICE_SHORTAGE",
                    "Practice allocation is short by " + remainingPractice + " period(s).");
        } else if (remainingPractice < 0) {
            // Trường hợp DƯ
            result.addError("PRACTICE_SURPLUS",
                    "Practice allocation exceeded by " + Math.abs(remainingPractice) + " period(s).");
        }

        // -- Validate Tự học (Self-study) --
        if (remainingSelfStudy > 0) {
            // Trường hợp THIẾU
            result.addError("SELF_STUDY_SHORTAGE",
                    "Self-study allocation is short by " + remainingSelfStudy + " hour(s).");
        } else if (remainingSelfStudy < 0) {
            // Trường hợp DƯ
            result.addError("SELF_STUDY_SURPLUS",
                    "Self-study allocation exceeded by " + Math.abs(remainingSelfStudy) + " hour(s).");
        }

        return result;
    }
}
