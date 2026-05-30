package com.example.smd.services;

import com.example.smd.dto.response.pdf.PdfSubjectExtractDTO;
import com.example.smd.dto.response.pdf.ProgramInfoDTO;
import com.example.smd.dto.response.pdf.ReferenceBookExtractDTO;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.PloStatus;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

/**
 * Service thuần (không AI) để đọc file PDF chương trình đào tạo
 * và trích xuất danh sách môn học & tài liệu tham khảo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExtractService {

    // ── Patterns cho Subject ──────────────────────────────────────────────────
    private static final Pattern SECTION_7_START = Pattern.compile(
            "7[\\s\\p{Z}\\p{Cf}]*\\.?[\\s\\p{Z}\\p{Cf}]*program[\\s\\p{Z}\\p{Cf}]+content",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NEXT_SECTION = Pattern.compile(
            "^[89]\\d*[.\\s].+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DESCRIPTION_SECTION = Pattern.compile(
            "^subject\\s+description",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("^\\d{4,10}$");

    private static final Pattern TIME_ALLOC = Pattern.compile(
            "\\(?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)?"
    );

    private static final Set<String> CATEGORY_MARKERS = Set.of(
            "SS", "GE", "PE", "CSE", "ISE", "EE", "ME", "CE", "IT", "BA",
            "MGT", "LAW", "MIS", "ACC", "FIN", "MKT", "HRM", "OB", "SCM",
            "MATH", "EN", "PHE", "IS", "SE", "AI", "DS", "CY"
    );

    private static final List<String> SUB_HEADER_KEYWORDS = List.of(
            "general education", "professional knowledge", "specialized knowledge",
            "graduation", "program content", "knowledge block", "subtotal",
            "total credits", "basic knowledge", "subject code", "subject name",
            "faculties", "departments", "number of", "theory, practical"
    );
    // ── Patterns cho Reference Book ───────────────────────────────────────────
    private static final Set<String> REF_TABLE_HEADERS = Set.of(
            "index", "reference code", "reference name", "author's name", "author name",
            "authors name", "publisher", "published year", "edition", "subject",
            "subject code", "no", "no.", "stt"
    );
    // Điểm neo nới lỏng: Dòng bắt đầu bằng (Optional Index) + 5-10 số (Code). Phía sau có chữ hoặc không đều được.
    private static final Pattern NEW_REF_ANCHOR = Pattern.compile("^(?:\\d{1,4}\\s+)?(\\d{5,10})(?:\\s+(.*))?$");
    private final MajorRepository majorRepository;
    private final RegulationRepository regulationRepository;



    // ── Public API — Subject Extraction ──────────────────────────────────────

    public String extractSubjectsFromPdf(MultipartFile file) throws IOException {
        return formatResult(extractSubjectDTOs(file));
    }

    @Transactional
    public String extractSubjectsAndReferenceFromPdf(MultipartFile file) throws IOException {
        byte[] pdfBytes = file.getBytes();

        ProgramInfoDTO programInfo;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            programInfo = extractProgramInfo(document);
        }

        Major savedMajor = createOrGetMajor(programInfo);

        var subject = formatResult(extractSubjectDTOs(new ByteArrayInputStream(pdfBytes)));
        var reference = formatReferenceBooks(extractReferenceRawTable(new ByteArrayInputStream(pdfBytes)));
        
        List<Regulation> regulations = new ArrayList<>();

        regulations.add(createRegulation("COURSE_MAPPING", "Detailed Course Metrics (N|a|b|c)",
                subject, savedMajor));
        regulations.add(createRegulation("SOURCE_DOCUMENTS", "Main Textbooks and Reference List",
                reference, savedMajor));
        var result =
                "Major: " + String.valueOf(savedMajor.getMajorCode()) + " and " + String.valueOf(savedMajor.getMajorName())
                        +"\nSubjects: " + subject
                        + "\nReference Books: " + reference;
        regulationRepository.saveAll(regulations);
        return result;
    }

    private Major createOrGetMajor(ProgramInfoDTO programInfo) {
        Optional<Major> existing = majorRepository.findByMajorCode(programInfo.getProgramCode());
        if (existing.isPresent()) {
            return existing.get();
        }
        Major major = new Major();
        major.setMajorCode(programInfo.getProgramCode());
        major.setMajorName(programInfo.getProgramName());
        major.setStatus(PloStatus.DRAFT.toString());
        return majorRepository.save(major);
    }

    public List<PdfSubjectExtractDTO> extractSubjectDTOs(MultipartFile file) throws IOException {
        return extractSubjectDTOs(file.getInputStream());
    }

    public List<PdfSubjectExtractDTO> extractSubjectDTOs(InputStream inputStream) throws IOException {
        String rawText = extractRawText(inputStream);
        log.debug("[PDF-EXTRACT] PDFBox extracted {} chars", rawText.length());

        String normalized = normalizeUnicode(rawText);
        List<String> section7Lines = extractSection7Lines(normalized);
        log.debug("[PDF-EXTRACT] Section 7 line count: {}", section7Lines.size());

        List<PdfSubjectExtractDTO> result = parseRecords(section7Lines);
        log.info("[PDF-EXTRACT] Total subjects parsed: {}", result.size());
        return result;
    }

    // ── Public API — Reference Book Extraction ────────────────────────────────

    public List<ReferenceBookExtractDTO> extractReferenceBooks(MultipartFile file) throws IOException {
        List<ReferenceBookExtractDTO> result = extractReferenceRawTable(file.getInputStream());
        return result;
    }

    public String extractReferenceBooksFormatted(MultipartFile file) throws IOException {
        return formatReferenceBooks(extractReferenceBooks(file));
    }

    // ── Step 1: Đọc PDF ───────────────────────────────────────────────────────

    private String extractRawText(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Cấu hình này giúp đọc bảng Subject rất tốt nhưng trộn cột bảng Reference
            String text = stripper.getText(document);
            log.debug("[PDF-EXTRACT] PDFBox extracted {} pages", document.getNumberOfPages());
            return text;
        }
    }

    // ── Step 2: Chuẩn hoá Unicode ─────────────────────────────────────────────

    private String normalizeUnicode(String text) {
        if (text == null) return "";
        String nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return nfkc.replaceAll("\\p{Cf}", "");
    }

    // ── Step 3: Cắt section 7 (Subjects) ──────────────────────────────────────

    private List<String> extractSection7Lines(String normalizedText) {
        String[] allLines = normalizedText.split("\\r?\\n");
        List<String> result = new ArrayList<>();
        boolean insideSection7 = false;

        for (String line : allLines) {
            String trimmed = line.trim();
            if (!insideSection7) {
                if (SECTION_7_START.matcher(trimmed).find()) {
                    insideSection7 = true;
                }
                continue;
            }
            if (NEXT_SECTION.matcher(trimmed).find()) break;
            if (DESCRIPTION_SECTION.matcher(trimmed).find()) break;

            result.add(trimmed);
        }
        return result;
    }

    // ── Step 4: Header-line driven parser (Subjects) ──────────────────────────

    private List<PdfSubjectExtractDTO> parseRecords(List<String> lines) {
        Map<String, PdfSubjectExtractDTO> seen = new LinkedHashMap<>();

        String currentCode       = null;
        String currentPartial    = null;
        String currentCredits    = "";
        String currentTheory     = null;
        String currentPractical  = null;
        String currentSelfStudy  = null;
        String currentSemester   = "";
        List<String> continuations = new ArrayList<>();

        for (String line : lines) {
            Matcher timeMatcher = TIME_ALLOC.matcher(line);
            if (timeMatcher.find()) {
                if (currentCode != null) {
                    PdfSubjectExtractDTO dto = buildDto(currentCode, currentPartial, continuations,
                            currentCredits, currentTheory, currentPractical, currentSelfStudy, currentSemester);
                    if (dto != null) seen.putIfAbsent(currentCode, dto);
                }

                currentTheory    = timeMatcher.group(1).trim();
                currentPractical = timeMatcher.group(2).trim();
                currentSelfStudy = timeMatcher.group(3).trim();

                String beforeTime = line.substring(0, timeMatcher.start()).trim();
                String afterTime  = line.substring(timeMatcher.end()).trim();

                currentSemester = "";
                Matcher semSearch = Pattern.compile("\\d+").matcher(afterTime);
                if (semSearch.find()) currentSemester = semSearch.group();

                HeaderParts hp = parseHeaderLine(beforeTime);
                currentCode    = hp.code;
                currentPartial = hp.partialName;
                currentCredits = hp.credits;
                continuations.clear();
            } else if (currentCode != null && !line.isEmpty()) {
                continuations.add(line);
            }
        }

        if (currentCode != null) {
            PdfSubjectExtractDTO dto = buildDto(currentCode, currentPartial, continuations,
                    currentCredits, currentTheory, currentPractical, currentSelfStudy, currentSemester);
            if (dto != null) seen.putIfAbsent(currentCode, dto);
        }

        return new ArrayList<>(seen.values());
    }

    private HeaderParts parseHeaderLine(String beforeTime) {
        if (beforeTime == null || beforeTime.isBlank()) return new HeaderParts("", "", "");

        String[] tokens = beforeTime.trim().split("\\s+");
        String code = "";
        String credits = "";
        int codeIdx = -1;
        int creditsIdx = -1;

        for (int i = 0; i < tokens.length; i++) {
            if (SUBJECT_CODE_PATTERN.matcher(tokens[i]).matches()) {
                codeIdx = i;
                code    = tokens[i];
                break;
            }
        }
        if (codeIdx < 0) return new HeaderParts("", "", "");

        for (int i = tokens.length - 1; i > codeIdx; i--) {
            if (tokens[i].matches("\\d{1,3}")) {
                creditsIdx = i;
                credits    = tokens[i];
                break;
            }
        }

        List<String> nameParts = new ArrayList<>();
        for (int i = codeIdx + 1; i < tokens.length; i++) {
            if (i == creditsIdx) continue;
            String t = tokens[i].trim();
            if (t.isEmpty() || CATEGORY_MARKERS.contains(t.toUpperCase())) continue;
            nameParts.add(t);
        }

        return new HeaderParts(code, String.join(" ", nameParts), credits);
    }

    private PdfSubjectExtractDTO buildDto(String code, String partialName, List<String> continuations,
                                          String credits, String theory, String practical, String selfStudy, String semester) {
        if (code == null || code.isBlank()) return null;

        List<String> allParts = new ArrayList<>();
        if (partialName != null && !partialName.isBlank()) allParts.add(partialName.trim());

        for (String cont : continuations) {
            String trimmed = cont.trim();
            if (trimmed.isEmpty() || CATEGORY_MARKERS.contains(trimmed.toUpperCase())) continue;
            allParts.add(trimmed);
        }

        String fullName = String.join(" ", allParts).trim().replaceAll("\\s{2,}", " ");
        if (isSubHeaderRow(fullName) || fullName.isEmpty()) return null;

        return PdfSubjectExtractDTO.builder()
                .subjectCode(code)
                .subjectName(fullName)
                .expectedSemester(semester != null ? semester : "")
                .numberOfCredits(credits != null ? credits : "")
                .theory(theory   != null ? theory   : "")
                .practical(practical != null ? practical : "")
                .selfStudy(selfStudy != null ? selfStudy : "")
                .build();
    }

    private String formatResult(List<PdfSubjectExtractDTO> subjects) {
        if (subjects.isEmpty()) return "";
        return subjects.stream().map(PdfSubjectExtractDTO::toFormattedString).collect(Collectors.joining(", "));
    }

    private boolean isSubHeaderRow(String name) {
        if (name == null || name.isBlank()) return true;
        return SUB_HEADER_KEYWORDS.stream().anyMatch(name.toLowerCase()::contains);
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // REFERENCE BOOK EXTRACTION (Tối ưu hóa Header-Line Driven)
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ReferenceBookExtractDTO> extractReferenceRawTable(InputStream is) throws IOException {
        List<ReferenceBookExtractDTO> result = new ArrayList<>();
        File tempFile = Files.createTempFile("smd_reference_", ".pdf").toFile();

        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                is.transferTo(fos);
            }
            
            try (PDDocument document = PDDocument.load(tempFile);
                 ObjectExtractor extractor = new ObjectExtractor(document)) {
                 
                PDFTextStripper stripper = new PDFTextStripper();
                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
                PageIterator it = extractor.extract();

                boolean insideRefSection = false;
                
                while (it.hasNext()) {
                    Page page = it.next();
                    
                    stripper.setStartPage(page.getPageNumber());
                    stripper.setEndPage(page.getPageNumber());
                    String pageText = stripper.getText(document).toLowerCase();
                    
                    if (!insideRefSection) {
                        boolean hasContentMarkers = pageText.contains("b) books, textbooks") ||
                                                    pageText.contains("list of reference books") ||
                                                    pageText.contains("reference code") ||
                                                    pageText.contains("index reference code");
                                                    
                        // Exclude TOC by ensuring it's not a TOC line with dots, unless it explicitly contains table headers.
                        boolean isToc = pageText.matches("(?s).*\\.{5,}\\s*\\d+.*") && !pageText.contains("reference code");

                        boolean isRefPage = hasContentMarkers && !isToc;

                        log.info("PAGE={} IS_REFERENCE_PAGE={}", page.getPageNumber(), isRefPage);

                        if (isRefPage) {
                            insideRefSection = true;
                            log.info("[PDF-REFERENCE] MATCHED REFERENCE SECTION PAGE={}", page.getPageNumber());
                        } else {
                            continue;
                        }
                    }
                    
                    List<Table> tables = sea.extract(page);
                    if (tables.isEmpty()) {
                        tables = bea.extract(page); 
                    }
                    log.info(
                            "[PDF-REFERENCE] PAGE {} -> TABLE COUNT={}",
                            page.getPageNumber(),
                            tables.size()
                    );
                    
                    for (Table table : tables) {

                        for (List<RectangularTextContainer> row : table.getRows()) {
                            List<String> cells = row.stream()
                                    .map(c -> c.getText()
                                            .replace("\r", " ")
                                            .replace("\n", " ")
                                            .trim())
                                    .toList();

                            log.info(
                                    "[PDF-REFERENCE] ROW(size={}) => {}",
                                    row.size(),
                                    cells
                            );
                            if (row.size() >= 3) {
                                // Cố gắng quét tìm cột Code trước (thường là cột 1 hoặc cột 2 do parse lỗi)
                                int codeIdx = -1;
                                for (int i = 0; i < Math.min(3, row.size()); i++) {
                                    String text = row.get(i).getText().replace("\r", " ").replace("\n", " ").trim();
                                    if (text.matches("^\\d{5,10}$")) {
                                        codeIdx = i;
                                        break;
                                    }
                                }

                                if (codeIdx != -1) {
                                    String colCode = row.get(codeIdx).getText().replace("\r", " ").replace("\n", " ").trim();
                                    String colName = codeIdx + 1 < row.size() ? row.get(codeIdx + 1).getText().replace("\r", " ").replace("\n", " ").trim() : "";
                                    String colAuthor = codeIdx + 2 < row.size() ? row.get(codeIdx + 2).getText().replace("\r", " ").replace("\n", " ").trim() : "";
                                    String colPublisher = codeIdx + 3 < row.size() ? row.get(codeIdx + 3).getText().replace("\r", " ").replace("\n", " ").trim() : "";
                                    String colYear = codeIdx + 4 < row.size() ? row.get(codeIdx + 4).getText().replace("\r", " ").replace("\n", " ").trim() : "";
                                    // Bỏ qua cột Edition (codeIdx + 5) và Subject (codeIdx + 6)
                                    String colSubjectCode =
                                            row.size() >= 8
                                                    ? row.get(row.size() - 1)
                                                    .getText()
                                                    .replace("\r", " ")
                                                    .replace("\n", " ")
                                                    .trim()
                                                    .replaceAll("\\s*,\\s*", ", ")
                                                    .replaceAll(",\\s*,+", ", ")
                                                    : "";
                                    result.add(ReferenceBookExtractDTO.builder()
                                            .referenceCode(normalizeAndClean(colCode))
                                            .referenceName(normalizeAndClean(colName))
                                            .authorName(normalizeAndClean(colAuthor))
                                            .publisher(normalizeAndClean(colPublisher))
                                            .publishedYear(normalizeAndClean(colYear))
                                            .subjectCode(normalizeAndClean(colSubjectCode))
                                            .build());
                                }else {
                                    List<String> cells1 = row.stream()
                                            .map(c -> c.getText()
                                                    .replace("\r", " ")
                                                    .replace("\n", " ")
                                                    .trim())
                                            .toList();

                                    log.warn(
                                            "[PDF-REFERENCE] NO REFERENCE CODE FOUND IN ROW => {}",
                                            cells1
                                    );
                                }

                            }
                        }
                    }
                    
                    if (pageText.contains("list of monographs") || 
                        Pattern.compile("(?i)(appendix|accreditation|curriculum\\s+mapping|notes:|note:)").matcher(pageText).find()) {
                        log.info("[PDF-REFERENCE] Kết thúc section tài liệu tham khảo tại trang {}", page.getPageNumber());
                        break; 
                    }
                }
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        
        Map<String, ReferenceBookExtractDTO> dedup = new LinkedHashMap<>();
        for (ReferenceBookExtractDTO dto : result) {
            dedup.putIfAbsent(dto.getReferenceCode(), dto);
        }
        return new ArrayList<>(dedup.values());
    }

    private String normalizeAndClean(String text) {
        if (text == null) return "";
        String nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC);
        nfkc = nfkc.replaceAll("\\p{Cf}", ""); 
        return cleanText(nfkc);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s{2,}", " ");
    }

    public String formatReferenceBooks(List<ReferenceBookExtractDTO> refs) {
        if (refs == null || refs.isEmpty()) return "";
        return refs.stream()
                .map(ReferenceBookExtractDTO::toFormattedString)
                .collect(Collectors.joining(", "));
    }
    private ProgramInfoDTO extractProgramInfo(PDDocument document) throws IOException {

        PDFTextStripper stripper = new PDFTextStripper();

        // Chỉ đọc vài trang đầu
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(3, document.getNumberOfPages()));

        String text = normalizeUnicode(stripper.getText(document));

        String industry = "";
        String code = "";

        // =====================================================
        // CASE 1:
        // Industry: INSURANCE
        // Code: 7340204
        // =====================================================
        Pattern industryPattern = Pattern.compile(
                "(?is)industry\\s*:\\s*(.*?)\\s*(?=code\\s*:|\\r?\\n|$)"
        );

        Matcher industryMatcher = industryPattern.matcher(text);

        if (industryMatcher.find()) {
            industry = industryMatcher.group(1)
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        // =====================================================
        // CASE 2:
        // Industry Name
        // INSURANCE
        // Code: 7340204
        // =====================================================
        if (industry.isBlank()) {

            Pattern industryNamePattern = Pattern.compile(
                    "(?is)industry\\s+name\\s*(.*?)\\s*code\\s*:"
            );

            Matcher matcher = industryNamePattern.matcher(text);

            if (matcher.find()) {
                industry = matcher.group(1)
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        }

        // =====================================================
        // CASE 3:
        // INSURANCE INDUSTRY TRAINING SCHEME
        // =====================================================
        if (industry.isBlank()) {

            Pattern trainingSchemePattern = Pattern.compile(
                    "(?im)^\\s*([A-Z][A-Z\\s&\\-]{2,})\\s+INDUSTRY\\s+TRAINING\\s+SCHEME"
            );

            Matcher matcher = trainingSchemePattern.matcher(text);

            if (matcher.find()) {
                industry = matcher.group(1)
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        }

        // =====================================================
        // PROGRAM CODE
        // =====================================================
        Pattern codePattern = Pattern.compile(
                "(?i)code\\s*:\\s*(\\d{4,10})"
        );

        Matcher codeMatcher = codePattern.matcher(text);

        if (codeMatcher.find()) {
            code = codeMatcher.group(1).trim();
        }

        log.info("[PDF-PROGRAM] Industry={}", industry);
        log.info("[PDF-PROGRAM] Code={}", code);

        return ProgramInfoDTO.builder()
                .programName(industry)
                .programCode(code)
                .build();
    }    // ── Inner value types ─────────────────────────────────────────────────────

    private record HeaderParts(String code, String partialName, String credits) {}

    private Regulation createRegulation(String code, String name, String value, Major major) {
        Regulation reg = new Regulation();
        reg.setCode(code);
        reg.setName(name);
        reg.setValue(value != null ? value : "N/A"); // Tránh Null cho trường @NotNull
        reg.setMajor(major);
        return reg;
    }
}