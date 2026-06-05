package com.example.smd.services;

import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Session;
import com.example.smd.entities.Subject;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SessionMapper;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SystemSettingService systemSettingService;
    private final AccountService accountService;
    private final SessionRepository sessionRepository;
    private final SyllabusRepository syllabusRepository;
    private final SessionMapper sessionMapper;
    private final SubjectRepository subjectRepository;
    private final BlockRepository blockRepository;

    private static final double SIMILARITY_THRESHOLD = 0.90;

    @Transactional(readOnly = true)
    public Page<SessionResponse> getAllSessions(UUID syllabusId,
                                                String search,
                                                int page,
                                                int size,
                                                String[] sort
    ) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] parsedSort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(parsedSort[1]), parsedSort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }


        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        Specification<Session> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            var syllabusJoin = root.join("syllabus", jakarta.persistence.criteria.JoinType.LEFT);

            if (syllabusId != null) {
                predicates.add(cb.equal(syllabusJoin.get("syllabusId"), syllabusId));
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
            if (!SyllabusStatus.PUBLISHED.toString().equalsIgnoreCase(session.getSyllabus().getStatus())) {
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
                return syllabusRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
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
    public boolean deleteSession(UUID sessionId, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        sessionRepository.delete(session);
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

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

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

    public SessionValidationResult validate(List<SessionRequest> inputs, UUID syllabusId) {
        if(inputs == null || inputs.isEmpty()) {
            throw new AppException(ErrorCode.SESSION_LIST_REQUIRED);
        }
        SessionValidationResult result = validateSessionType(inputs, syllabusId);
        result.setWarnings(validateContentSession(inputs, syllabusId));
        return result;
    }

    private SessionValidationResult validateSessionType(List<SessionRequest> inputs, UUID syllabusId) {
        var setting = systemSettingService.getDetailByCode("SESSION_MINUTE");
        var duration = Integer.parseInt(setting.getValue());
        SessionValidationResult result = new SessionValidationResult();

        Syllabus syllabus = syllabusRepository.findByIdWithSubject(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        Subject masterSubject = subjectRepository.findById(syllabus.getSubject().getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // LẤY DỮ LIỆU HIỆN TẠI TỪ DATABASE
        List<Session> existingDbSessions = sessionRepository.findBySyllabus_SyllabusId(syllabusId);
        // 1. Tính quỹ Lý thuyết (Quy đổi an toàn từ Giờ -> Tiết)
        double inputTotalTheoryHours = inputs.stream()
                .filter(s -> "THEORY".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        double dbTotalTheoryHours = existingDbSessions.stream()
                .filter(s -> "THEORY".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        int inputTotalTheoryPeriods = (int) Math.round(inputTotalTheoryHours / duration);
        int dbTotalTheoryPeriods = (int) Math.round(dbTotalTheoryHours / duration);
        int remainingTheory = (masterSubject.getTheoryPeriods() != null ? masterSubject.getTheoryPeriods() : 0) - inputTotalTheoryPeriods - dbTotalTheoryPeriods;

        // 2. Tính quỹ Thực hành (Tương tự)
        double inputTotalPracticeHours = inputs.stream()
                .filter(s -> "PRACTICE".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        double dbTotalPracticeHours = existingDbSessions.stream()
                .filter(s -> "PRACTICE".equalsIgnoreCase(s.getSessionType()))
                .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                .sum();
        int inputTotalPracticePeriods = (int) Math.round(inputTotalPracticeHours / duration);
        int dbTotalPracticePeriods = (int) Math.round(dbTotalPracticeHours / duration);
        int remainingPractice = (masterSubject.getPracticalPeriods() != null ? masterSubject.getPracticalPeriods() : 0) - inputTotalPracticePeriods - dbTotalPracticePeriods;

        // (Tùy chọn) Tính tổng giờ tự học nếu có bắt validate
//        int inputTotalSelfStudyHours = inputs.stream()
//                .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
//                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
//                .sum();
//        int dbTotalSelfStudyHours = existingDbSessions.stream()
//                .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
//                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
//                .sum();
//        int remainingSelfStudy = (masterSubject.getSelfStudyPeriods() != null ? masterSubject.getSelfStudyPeriods() * 60 : 0) - inputTotalSelfStudyHours - dbTotalSelfStudyHours;

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

//        // -- Validate Tự học (Self-study) --
//        if (remainingSelfStudy > 0) {
//            // Trường hợp THIẾU
//            result.addError("SELF_STUDY_SHORTAGE",
//                    "Self-study allocation is short by " + remainingSelfStudy + " minute(s).");
//        } else if (remainingSelfStudy < 0) {
//            // Trường hợp DƯ
//            result.addError("SELF_STUDY_SURPLUS",
//                    "Self-study allocation exceeded by " + Math.abs(remainingSelfStudy) + " minute(s).");
//        }

        return result;
    }

    private List<SessionValidationResult.ContentLineValidationError> validateContentSession(List<SessionRequest> inputs, UUID syllabusId) {
        List<SessionValidationResult.ContentLineValidationError> result = new ArrayList<>();

        List<Blocks> allBlocks = blockRepository.findAllBlocksBySyllabusIdUrgent(syllabusId);
        if (allBlocks.isEmpty()) {
            throw new AppException(ErrorCode.BLOCK_LIST_EMPTY);
        }

        Map<String, List<Blocks>> dbHierarchy = new HashMap<>();
        String currentH1Key = null;

        for (Blocks block : allBlocks) {
            if ("H1".equalsIgnoreCase(block.getBlockStyle())) {
                currentH1Key = cleanString(block.getContentText());
                dbHierarchy.put(currentH1Key, new ArrayList<>());
            } else if ("H2".equalsIgnoreCase(block.getBlockType()) && currentH1Key != null) {
                dbHierarchy.get(currentH1Key).add(block);
            }
        }

        JaroWinklerSimilarity similarityMeasure = new JaroWinklerSimilarity();

        for (SessionRequest request : inputs) {
            String reqTitle = request.getSessionTitle();
            String reqTopic = request.getSessionTopic();

            if (reqTitle == null || reqTitle.trim().isEmpty()) {
                result.add(SessionValidationResult.ContentLineValidationError.builder()
                        .code("SESSION_CONTENT_LINE_MISMATCH")
                        .message("The reqTitle is empty.")
                        .sessionNumber(request.getSessionNumber())
                        .lineIndex(0)
                        .similarityScore(0.0)
                        .build());
                continue;
            }

            String cleanReqTitle = cleanString(reqTitle);
            String matchedH1KeyInDb = null;
            double highestH1Score = 0;

            for (String dbH1Key : dbHierarchy.keySet()) {
                double score = similarityMeasure.apply(cleanReqTitle, dbH1Key);
                if (score > highestH1Score) {
                    highestH1Score = score;
                    if (score >= SIMILARITY_THRESHOLD) {
                        matchedH1KeyInDb = dbH1Key;
                    }
                }
            }

            if (matchedH1KeyInDb == null) {
                result.add(SessionValidationResult.ContentLineValidationError.builder()
                        .code("H1_NOT_FOUND")
                        .message("No chapters in the document matching the title were found.")
                        .sessionNumber(request.getSessionNumber())
                        .lineIndex(0)
                        .lineContent(reqTitle)
                        .similarityScore(highestH1Score)
                        .build());
                continue;
            }

            List<Blocks> h2BlocksInDb = dbHierarchy.get(matchedH1KeyInDb);

            if (reqTopic == null || reqTopic.trim().isEmpty()) {
                result.add(SessionValidationResult.ContentLineValidationError.builder()
                        .code("SESSION_CONTENT_LINE_MISMATCH")
                        .message("The session topic is empty.")
                        .sessionNumber(request.getSessionNumber())
                        .lineIndex(1)
                        .similarityScore(0.0)
                        .build());
                continue;
            }

            List<String> requestSubTopics = parseSubTopics(reqTopic);
            for (String subTopic : requestSubTopics) {
                String cleanSubTopic = cleanString(subTopic);
                boolean isH2Matched = false;
                double highestH2Score = 0;

                for (Blocks dbH2Block : h2BlocksInDb) {
                    String cleanDbH2 = cleanString(dbH2Block.getContentText());
                    double score = similarityMeasure.apply(cleanSubTopic, cleanDbH2);

                    if (score > highestH2Score) {
                        highestH2Score = score;
                    }

                    if (score >= SIMILARITY_THRESHOLD) {
                        isH2Matched = true;
                        break;
                    }
                }

                // Nếu dòng H2 này trong Request gõ lệch hoàn toàn so với các H2 của H1 đó trong DB
                if (!isH2Matched) {
                    result.add(SessionValidationResult.ContentLineValidationError.builder()
                            .code("SESSION_CONTENT_LINE_MISMATCH")
                            .message(String.format("The heading '%s' in SessionTopic does not match any subheading of chapter '%s' in the document.", subTopic, reqTitle))
                            .sessionNumber(request.getSessionNumber())
                            .lineIndex(1)
                            .lineContent(reqTitle)
                            .similarityScore(highestH2Score)
                            .build());
                }
            }
        }
        return result;
    }

    /**
     * Hàm Helper 1: Làm sạch chuỗi, hạ chữ thường, xóa khoảng trắng thừa
     */
    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * Hàm Helper 2: Cắt chuỗi theo dòng và lọc lấy các dòng tiêu đề chính (X.Y.)
     */
    private List<String> parseSubTopics(String topic) {
        List<String> subTopics = new ArrayList<>();
        if (topic == null) return subTopics;

        // Tách đoạn văn thành các dòng độc lập qua ký tự xuống dòng (Enter)
        String[] lines = topic.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // Chỉ lấy những dòng bắt đầu bằng định dạng số mục (Ví dụ: "1.1.", "1.2 ")
            // Bỏ qua các dòng mô tả nội dung chi tiết dạng gạch đầu dòng "-" hoặc "*"
            if (!trimmed.isEmpty() && Pattern.matches("^\\d+\\.\\d+.*", trimmed)) {
                subTopics.add(trimmed);
            }
        }
        return subTopics;
    }
}
