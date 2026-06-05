package com.example.smd.services;

import jakarta.persistence.criteria.Predicate;
import com.example.smd.dto.excel.SessionImportDTO;
import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.dto.response.validate.SessionImportResult;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.entities.*;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SessionMapper;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SubjectRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
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
    private final CLOsRepository closRepository;
    private final CloSessionMappingRepository cloSessionMappingRepository;

    // ============================================================ //
    // READ / QUERY //
    // ============================================================ //

    @Transactional(readOnly = true)
    public Page<SessionResponse> getAllSessions(UUID syllabusId,
            String search,
            int page,
            int size,
            String[] sort) {
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
                        cb.like(cb.lower(cb.coalesce(root.get("teachingMethods"), "")), pattern)));
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

    // ============================================================ //
    // CREATE / UPDATE / DELETE //
    // ============================================================ //

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
    public List<SessionResponse> createSessionsBluk(List<SessionRequest> requests, String accountId) {
        // 1. Kiểm tra quyền (Chỉ cần check 1 lần cho toàn bộ request)
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        List<Session> sessionsToSave = new ArrayList<>();

        // Cache Syllabus lại để tránh gọi DB nhiều lần nếu các session đều thuộc chung
        // 1 Syllabus
        Map<UUID, Syllabus> syllabusCache = new HashMap<>();

        // Dùng Set để track các session_number đang được tạo trong cùng list này để
        // tránh duplicate
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
        List<Session> sessions = sessionRepository.findBySyllabus_SyllabusIdAndSessionNumberIn(syllabusId,
                distinctNumbers);

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
    // ============================================================ //
    // VALIDATE (QUOTA) //
    // ============================================================ //

    public SessionValidationResult validate(List<SessionRequest> inputs, UUID syllabusId) {
        if (inputs == null || inputs.isEmpty()) {
            throw new AppException(ErrorCode.SESSION_LIST_REQUIRED);
        }
        SessionValidationResult result = validateSessionType(inputs, syllabusId);
        result.addWarning(validateContentSession(inputs, syllabusId));
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
        int remainingTheory = (masterSubject.getTheoryPeriods() != null ? masterSubject.getTheoryPeriods() : 0)
                - inputTotalTheoryPeriods - dbTotalTheoryPeriods;

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
        int remainingPractice = (masterSubject.getPracticalPeriods() != null ? masterSubject.getPracticalPeriods() : 0)
                - inputTotalPracticePeriods - dbTotalPracticePeriods;

        // (Tùy chọn) Tính tổng giờ tự học nếu có bắt validate
        // int inputTotalSelfStudyHours = inputs.stream()
        // .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
        // .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
        // .sum();
        // int dbTotalSelfStudyHours = existingDbSessions.stream()
        // .filter(s -> "SELF_STUDY".equalsIgnoreCase(s.getSessionType()))
        // .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
        // .sum();
        // int remainingSelfStudy = (masterSubject.getSelfStudyPeriods() != null ?
        // masterSubject.getSelfStudyPeriods() * 60 : 0) - inputTotalSelfStudyHours -
        // dbTotalSelfStudyHours;

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

        // // -- Validate Tự học (Self-study) --
        // if (remainingSelfStudy > 0) {
        // // Trường hợp THIẾU
        // result.addError("SELF_STUDY_SHORTAGE",
        // "Self-study allocation is short by " + remainingSelfStudy + " minute(s).");
        // } else if (remainingSelfStudy < 0) {
        // // Trường hợp DƯ
        // result.addError("SELF_STUDY_SURPLUS",
        // "Self-study allocation exceeded by " + Math.abs(remainingSelfStudy) + "
        // minute(s).");
        // }

        return result;
    }

    private List<SessionValidationResult.ContentLineValidationError> validateContentSession(List<SessionRequest> inputs,
            UUID syllabusId) {
        List<SessionValidationResult.ContentLineValidationError> result = new ArrayList<>();

        List<Blocks> allBlocks = blockRepository.findAllBlocksBySyllabusIdUrgent(syllabusId);
        if (allBlocks == null || allBlocks.isEmpty()) {
            log.warn("=== BUG CHECK: Danh sách allBlocks trống rỗng (0 phần tử) ===");
        } else {
            log.warn("=== BUG CHECK: Tìm thấy {} blocks ===", allBlocks.size());
            allBlocks.forEach(b -> log.warn("Block ID: {} | Style: {} | Type: {} | Content: {}",
                    b.getBlockId(), b.getBlockStyle(), b.getBlockType(), b.getContentText()));
        }
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

                // Nếu dòng H2 này trong Request gõ lệch hoàn toàn so với các H2 của H1 đó trong
                // DB
                if (!isH2Matched) {
                    result.add(SessionValidationResult.ContentLineValidationError.builder()
                            .code("SESSION_CONTENT_LINE_MISMATCH")
                            .message(String.format(
                                    "The heading '%s' in SessionTopic does not match any subheading of chapter '%s' in the document.",
                                    subTopic, reqTitle))
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
        if (input == null)
            return "";
        return input.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * Hàm Helper 2: Cắt chuỗi theo dòng và lọc lấy các dòng tiêu đề chính (X.Y.)
     */
    private List<String> parseSubTopics(String topic) {
        List<String> subTopics = new ArrayList<>();
        if (topic == null)
            return subTopics;

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
    // ============================================================ //
    // IMPORT SESSION FROM EXCEL //
    // ============================================================ //

    /**
     * Nhận file Excel và import danh sách Session vào Syllabus.
     *
     * <p>
     * <b>Business Flow:</b>
     * <ol>
     * <li>Đọc và parse file Excel thành {@link SessionImportDTO}.</li>
     * <li>Validate CLO — kiểm tra mã CLO trong file có tồn tại trong Subject hay
     * không.</li>
     * <li>Validate Quota — gọi hàm {@link #validate} để kiểm tra số tiết lý
     * thuyết/thực hành.</li>
     * <li>Nếu có bất kỳ lỗi nào → trả về ngay, KHÔNG lưu DB.</li>
     * <li>Nếu hợp lệ → xem comment bên trong về chiến lược Replace/Upsert.</li>
     * </ol>
     *
     * @param file       File Excel (.xlsx) được upload từ FE
     * @param syllabusId UUID của Syllabus cần import vào
     * @param subjectId  UUID của Subject (dùng để lấy danh sách CLO hợp lệ)
     * @return {@link SessionImportResult} chứa danh sách lỗi hoặc kết quả success
     */
    @Transactional
    public SessionImportResult importSessionsFromExcel(MultipartFile file,
            UUID syllabusId,
            UUID subjectId) {
        // ── Bước 0: Validate tham số đầu vào ─────────────────────────────
        Syllabus syllabus = syllabusRepository.findByIdWithSubject(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        // Lấy duration từ SystemSetting
        var setting = systemSettingService.getDetailByCode("SESSION_MINUTE");
        int sessionMinutes = Integer.parseInt(setting.getValue());

        // ── Bước 1: Đọc file Excel ────────────────────────────────────────
        List<SessionImportDTO> importRows;
        try {
            importRows = parseExcelFile(file, sessionMinutes);
        } catch (IOException e) {
            log.error("Failed to read Excel file: {}", e.getMessage(), e);
            SessionImportResult result = new SessionImportResult();
            result.addError("FILE_READ_ERROR",
                    "Cannot read the Excel file. Please ensure it is a valid .xlsx file.", null);
            return result;
        }

        SessionImportResult result = new SessionImportResult();
        result.setTotalRows(importRows.size());

        if (importRows.isEmpty()) {
            result.addError("EMPTY_FILE", "The Excel file contains no data rows.", null);
            return result;
        }

        // ── Bước 2a: Validate CLO ─────────────────────────────────────────
        validateCloMappings(importRows, subjectId, result);

        // ── Bước 2b: Validate Quota (tận dụng hàm validate() có sẵn) ─────
        // Map sang List<SessionRequest> để truyền vào hàm validate
        // Lưu ý: validate() sẽ đọc existingDbSessions từ DB. Trong luồng REPLACE,
        // cần xóa session cũ trước khi validate để số tiết không bị tính đôi.
        // Ở đây ta validate TRƯỚC khi xóa, nên existingDbSessions = [] nếu syllabus
        // chưa có session,
        // hoặc phải truyền vào list trống nếu muốn validate độc lập.
        List<SessionRequest> sessionRequests = mapToSessionRequests(importRows, syllabusId, sessionMinutes);
        SessionValidationResult quotaResult = validate(sessionRequests, syllabusId);

        // Merge lỗi quota vào result tổng
        result.mergeErrors(quotaResult);

        // ── Bước 3: Nếu có lỗi → Return sớm, KHÔNG lưu DB ───────────────
        if (!result.isValid()) {
            return result;
        }

        // ── Bước 4: Lưu DB (Transactional) ───────────────────────────────
        // TODO [DECISION POINT - CẦN XEM XÉT]:
        // Hiện tại đang dùng chiến lược "REPLACE" (xóa toàn bộ Session cũ rồi insert
        // mới).
        // Hãy comment/uncomment block tùy theo quyết định:
        //
        // Chiến lược A - REPLACE (xóa cũ, thêm mới): ← ĐANG DÙNG
        // Ưu điểm : Đơn giản, đảm bảo dữ liệu sạch, không conflict số Session.
        // Nhược điểm: Mất toàn bộ Session cũ (kể cả Session không có trong file).
        //
        // Chiến lược B - UPSERT (giữ Session cũ, chỉ thêm/cập nhật Session trong file):
        // Ưu điểm : An toàn hơn, không mất dữ liệu ngoài file.
        // Nhược điểm: Logic phức tạp hơn, cần match theo sessionNumber.

        // [REPLACE] Xóa CLO mapping trước, sau đó xóa Session cũ (tránh FK constraint)
        cloSessionMappingRepository.deleteBySession_Syllabus_SyllabusId(syllabusId);
        sessionRepository.deleteAllBySyllabus_SyllabusId(syllabusId);
        sessionRepository.flush(); // Đảm bảo DELETE được flush trước INSERT

        // Lưu Session và CLO mapping mới
        int savedCount = saveSessionsAndMappings(importRows, syllabus, subjectId);
        result.setSavedCount(savedCount);
        return result;
    }

    // ============================================================ //
    // PRIVATE HELPERS //
    // ============================================================ //

    /**
     * Đọc file Excel và map từng dòng thành {@link SessionImportDTO}.
     *
     * <p>
     * Cấu trúc cột file Excel (0-indexed):
     * 
     * <pre>
     *   Col 0: Session Number  — Số thứ tự buổi học (số nguyên)
     *   Col 1: Title           — Tiêu đề/Chương
     *   Col 2: Teaching Methods— Phương pháp giảng dạy
     *   Col 3: Topic           — Nội dung/Chủ đề
     *   Col 4: Type            — THEORY | PRACTICE | SELF_STUDY
     *   Col 5: CLO-Mapping     — Danh sách mã CLO, cách nhau bằng dấu phẩy (VD: "CLO1, CLO2")
     * </pre>
     *
     * @param sessionMinutes tham số unused nhưng để dành cho mở rộng sau
     */
    private List<SessionImportDTO> parseExcelFile(MultipartFile file, int sessionMinutes) throws IOException {
        List<SessionImportDTO> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Bắt đầu từ dòng index 1 để bỏ qua dòng header (index 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row))
                    continue;

                Integer sessionNumber = (int) getNumericCellValue(row, 0);
                String sessionTitle = getStringCellValue(row, 1);
                String teachingMethods = getStringCellValue(row, 2);
                String sessionTopic = getStringCellValue(row, 3);
                String sessionType = getStringCellValue(row, 4).toUpperCase().trim();
                String cloMappingRaw = getStringCellValue(row, 5);

                // Tách và chuẩn hoá danh sách mã CLO
                List<String> cloCodes = Arrays.stream(cloMappingRaw.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                rows.add(SessionImportDTO.builder()
                        .sessionNumber(sessionNumber)
                        .sessionTitle(sessionTitle)
                        .teachingMethods(teachingMethods)
                        .sessionTopic(sessionTopic)
                        .sessionType(sessionType)
                        .cloCodes(cloCodes)
                        .rowIndex(i + 1) // 1-indexed để FE hiển thị số dòng cho người dùng
                        .build());
            }
        }

        return rows;
    }

    /**
     * Validate toàn bộ mã CLO trong danh sách import.
     * Truy vấn DB một lần để lấy tất cả CLO Code hợp lệ thuộc Subject,
     * sau đó duyệt từng dòng để kiểm tra.
     * Lỗi được gộp vào {@code result} — KHÔNG throw exception để gom hết lỗi một
     * lần.
     */
    private void validateCloMappings(List<SessionImportDTO> importRows,
            UUID subjectId,
            SessionImportResult result) {
        // Tải 1 lần toàn bộ CLO hợp lệ của Subject
        Set<String> validCloCodes = closRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .map(clo -> clo.getCloCode().toUpperCase().trim())
                .collect(Collectors.toSet());

        for (SessionImportDTO row : importRows) {
            if (row.getCloCodes() == null || row.getCloCodes().isEmpty())
                continue;

            for (String cloCode : row.getCloCodes()) {
                if (!validCloCodes.contains(cloCode)) {
                    result.addError(
                            "CLO_INVALID",
                            String.format("Row %d (Session %d): CLO code '%s' does not belong to this Subject.",
                                    row.getRowIndex(), row.getSessionNumber(), cloCode),
                            row.getRowIndex());
                }
            }
        }
    }

    /**
     * Map từ {@link SessionImportDTO} sang {@link SessionRequest} để truyền vào
     * hàm {@link #validate(List, UUID)} — kiểm tra quota tiết lý thuyết / thực
     * hành.
     * Duration = sessionMinutes (1 session tương đương 1 tiết = SESSION_MINUTE
     * phút).
     */
    private List<SessionRequest> mapToSessionRequests(List<SessionImportDTO> importRows,
            UUID syllabusId,
            int sessionMinutes) {
        return importRows.stream()
                .map(row -> SessionRequest.builder()
                        .syllabusId(syllabusId)
                        .sessionNumber(row.getSessionNumber())
                        .sessionTitle(row.getSessionTitle())
                        .teachingMethods(row.getTeachingMethods())
                        .sessionTopic(row.getSessionTopic())
                        .sessionType(row.getSessionType())
                        .duration(sessionMinutes) // 1 session = 1 tiết = SESSION_MINUTE phút
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lưu danh sách Session và CLO_Session mapping xuống DB.
     * Được gọi SAU KHI toàn bộ validate đã pass và Session cũ đã bị xóa
     * (replace-mode).
     * Tải trước toàn bộ CLO entity để tránh N+1 query.
     *
     * @return Số Session đã lưu thành công
     */
    private int saveSessionsAndMappings(List<SessionImportDTO> importRows,
            Syllabus syllabus,
            UUID subjectId) {
        // Pre-load toàn bộ CLO entity một lần (tránh N+1)
        Map<String, CLOs> cloMap = closRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .collect(Collectors.toMap(
                        clo -> clo.getCloCode().toUpperCase().trim(),
                        clo -> clo));

        int savedCount = 0;

        for (SessionImportDTO row : importRows) {
            // Xây dựng Session entity
            Session session = Session.builder()
                    .syllabus(syllabus)
                    .sessionNumber(row.getSessionNumber())
                    .sessionTitle(row.getSessionTitle())
                    .teachingMethods(row.getTeachingMethods())
                    .sessionTopic(row.getSessionTopic())
                    .sessionType(row.getSessionType())
                    // duration: theo yêu cầu, bám theo SystemSetting SESSION_MINUTE
                    // Nếu muốn lấy duration từ file Excel thì đọc từ row và gán ở đây
                    .build();

            Session saved = sessionRepository.save(session);
            savedCount++;

            // Lưu CLO_Session mapping
            if (row.getCloCodes() != null && !row.getCloCodes().isEmpty()) {
                List<CLO_Session> mappings = row.getCloCodes().stream()
                        .map(cloCode -> cloMap.get(cloCode.toUpperCase().trim()))
                        .filter(Objects::nonNull)
                        .map(clo -> CLO_Session.builder()
                                .clo(clo)
                                .session(saved)
                                .build())
                        .collect(Collectors.toList());

                cloSessionMappingRepository.saveAll(mappings);
            }
        }

        return savedCount;
    }

    // ============================================================ //
    // EXCEL CELL HELPERS //
    // ============================================================ //

    /** Trả về giá trị số của cell. Trả về 0 nếu cell null hoặc không phải số. */
    private double getNumericCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    /** Trả về giá trị String của cell. Trả về chuỗi rỗng nếu cell null. */
    private String getStringCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Kiểm tra xem một Row có rỗng hoàn toàn không (tất cả cell đều blank/null).
     */
    private boolean isRowEmpty(Row row) {
        if (row == null)
            return true;

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                // [FIX] Bổ sung: Nếu là kiểu chuỗi nhưng cắt khoảng trắng đi mà rỗng thì vẫn
                // coi là Blank
                if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty()) {
                    continue;
                }
                return false; // Có ít nhất 1 ô chứa dữ liệu thực sự
            }
        }
        return true;
    }
}
