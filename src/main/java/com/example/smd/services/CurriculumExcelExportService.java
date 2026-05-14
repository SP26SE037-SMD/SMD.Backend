package com.example.smd.services;

import com.example.smd.entities.*;
import com.example.smd.repositories.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurriculumExcelExportService {

    CurriculumRepository curriculumRepository;
    MajorRepository majorRepository;
    POsRepository posRepository;
    PLOsRepository plosRepository;
    PoPloMappingRepository poPloMappingRepository;
    CLOsRepository closRepository;
    CloPloMappingRepository cloPloMappingRepository;
    CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;
    PrerequisiteRepository prerequisiteRepository;

    // =========================================================
    //  PUBLIC ENTRY POINT
    // =========================================================

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportFullCurriculum(UUID curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new RuntimeException("Curriculum not found: " + curriculumId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Build shared styles
            Styles styles = new Styles(workbook);

            writeMajorSheet(workbook, styles, curriculum);
            writeCurriculumSheet(workbook, styles, curriculum);
            writeSubjectSheet(workbook, styles, curriculum);
            writeCloPloMappingSheet(workbook, styles, curriculum);
            writeSemesterMappingSheet(workbook, styles, curriculum);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to export curriculum to Excel", e);
        }
    }

    // =========================================================
    //  SHEET 1 – MAJOR
    //  Columns: Major Code | Name | Description | PO Code | PO Description
    //  Rule: Merge cols 0-2 vertically per Major block
    // =========================================================

    private void writeMajorSheet(XSSFWorkbook wb, Styles s, Curriculum curriculum) {
        Sheet sheet = wb.createSheet("Major");
        Major major = curriculum.getMajor();

        // Header row
        String[] headers = {"Major Code", "Name", "Description", "PO Code", "Description"};
        writeHeaderRow(sheet, s, headers);

        List<PO> pos = posRepository.findByMajor_MajorId(major.getMajorId());

        int startRow = 1; // first data row index (0-based for sheet, 1-based after header)
        int rowIdx = 1;

        if (pos.isEmpty()) {
            // No POs – write single row for major with empty PO cols
            Row row = sheet.createRow(rowIdx++);
            writeCell(row, 0, major.getMajorCode(), s.data);
            writeCell(row, 1, major.getMajorName(), s.data);
            writeCell(row, 2, major.getDescription(), s.data);
            writeCell(row, 3, "", s.data);
            writeCell(row, 4, "", s.data);
        } else {
            for (int i = 0; i < pos.size(); i++) {
                PO po = pos.get(i);
                Row row = sheet.createRow(rowIdx++);

                if (i == 0) {
                    // First PO row – print major info
                    writeCell(row, 0, major.getMajorCode(), s.data);
                    writeCell(row, 1, major.getMajorName(), s.data);
                    writeCell(row, 2, major.getDescription(), s.data);
                } else {
                    // Subsequent PO rows – leave major cols empty (will be merged)
                    writeCell(row, 0, "", s.data);
                    writeCell(row, 1, "", s.data);
                    writeCell(row, 2, "", s.data);
                }
                writeCell(row, 3, po.getPoCode(), s.data);
                writeCell(row, 4, po.getDescription(), s.data);
            }

            // Merge major info columns (0, 1, 2) vertically for the block
            int endRow = rowIdx - 1;
            if (endRow > startRow) {
                // Merge col 0 (Major Code) from startRow to endRow
                sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));
                // Merge col 1 (Name)
                sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 1, 1));
                // Merge col 2 (Description)
                sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 2, 2));
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    // =========================================================
    //  SHEET 2 – CURRICULUM
    //  Columns: Curriculum Code | Name | Start Year | Description | PLO Code | PLO Desc | PO1 | PO2 ...
    //  Rule: Merge cols 0-3 vertically; mark "x" + green fill for PLO-PO mapping
    // =========================================================

    private void writeCurriculumSheet(XSSFWorkbook wb, Styles s, Curriculum curriculum) {
        Sheet sheet = wb.createSheet("Curriculum");
        Major major = curriculum.getMajor();

        // Load POs (columns after PLO desc are PO headers)
        List<PO> pos = posRepository.findByMajor_MajorId(major.getMajorId());
        // Load PLOs for this curriculum
        List<PLOs> plos = plosRepository.findByCurriculum_CurriculumId(curriculum.getCurriculumId());
        // Load PO-PLO mappings for this curriculum
        List<PO_PLO_Mapping> poPlomappings = poPloMappingRepository
                .findByPlo_Curriculum_CurriculumId(curriculum.getCurriculumId());

        // Build a Set<String> of "poId_ploId" for fast lookup
        Set<String> mappingSet = poPlomappings.stream()
                .map(m -> m.getPo().getPoId() + "_" + m.getPlo().getPloId())
                .collect(Collectors.toSet());

        // Build header: fixed 6 cols + dynamic PO cols
        int fixedCols = 6;
        String[] headers = new String[fixedCols + pos.size()];
        headers[0] = "Curriculum Code";
        headers[1] = "Name";
        headers[2] = "Start Year";
        headers[3] = "Description";
        headers[4] = "PLO Code";
        headers[5] = "Description";
        for (int i = 0; i < pos.size(); i++) {
            headers[fixedCols + i] = pos.get(i).getPoCode();
        }
        writeHeaderRow(sheet, s, headers);

        int startRow = 1;
        int rowIdx = 1;

        if (plos.isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            writeCurriculumFixedCols(row, curriculum, 0, s.data);
            writeCell(row, 4, "", s.data);
            writeCell(row, 5, "", s.data);
            for (int p = 0; p < pos.size(); p++) writeCell(row, fixedCols + p, "", s.data);
        } else {
            for (int i = 0; i < plos.size(); i++) {
                PLOs plo = plos.get(i);
                Row row = sheet.createRow(rowIdx++);

                if (i == 0) {
                    writeCurriculumFixedCols(row, curriculum, 0, s.data);
                } else {
                    // Leave fixed cols empty (merged later)
                    for (int c = 0; c < 4; c++) writeCell(row, c, "", s.data);
                }

                writeCell(row, 4, plo.getPloCode(), s.data);
                writeCell(row, 5, plo.getDescription(), s.data);

                // Matrix: mark "x" and fill green if PLO maps to PO
                for (int p = 0; p < pos.size(); p++) {
                    String key = pos.get(p).getPoId() + "_" + plo.getPloId();
                    if (mappingSet.contains(key)) {
                        writeCell(row, fixedCols + p, "x", s.greenMatrix);
                    } else {
                        writeCell(row, fixedCols + p, "", s.data);
                    }
                }
            }

            // Merge curriculum fixed cols (0-3) vertically
            int endRow = rowIdx - 1;
            if (endRow > startRow) {
                for (int c = 0; c < 4; c++) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, c, c));
                }
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    /** Write columns 0-3 (fixed curriculum info) into a row */
    private void writeCurriculumFixedCols(Row row, Curriculum c, int startCol, CellStyle style) {
        writeCell(row, startCol,     c.getCurriculumCode(), style);
        writeCell(row, startCol + 1, c.getCurriculumName(), style);
        writeCell(row, startCol + 2, c.getStartYear() != null ? String.valueOf(c.getStartYear()) : "", style);
        writeCell(row, startCol + 3, c.getDescription(), style);
    }

    // =========================================================
    //  SHEET 3 – SUBJECT
    //  Flat table – 15 columns, no merges
    // =========================================================

    private void writeSubjectSheet(XSSFWorkbook wb, Styles s, Curriculum curriculum) {
        Sheet sheet = wb.createSheet("Subject");

        String[] headers = {
            "Subject Code", "Subject Name", "Description", "Department Code",
            "Credits", "Degree Level", "Time Allocation", "Min To Pass",
            "Student Limit", "Student Tasks", "Scoring Scale", "Min Bloom Level",
            "Theory Periods", "Practical Periods", "Self Study Periods"
        };
        writeHeaderRow(sheet, s, headers);

        // Get all subjects in this curriculum (distinct)
        List<Curriculum_Group_Subject> cgsEntries =
                curriculumGroupSubjectRepository.findAllByCurriculumIdOrderBySemester(curriculum.getCurriculumId());

        // Collect distinct subjects preserving order
        LinkedHashMap<UUID, Subject> subjectMap = new LinkedHashMap<>();
        for (Curriculum_Group_Subject cgs : cgsEntries) {
            Subject sub = cgs.getSubject();
            subjectMap.putIfAbsent(sub.getSubjectId(), sub);
        }

        int rowIdx = 1;
        for (Subject sub : subjectMap.values()) {
            Row row = sheet.createRow(rowIdx++);

            writeCell(row, 0,  sub.getSubjectCode(), s.data);
            writeCell(row, 1,  sub.getSubjectName(), s.data);
            writeCell(row, 2,  sub.getDescription(), s.data);
            writeCell(row, 3,  sub.getDepartment() != null ? sub.getDepartment().getDepartmentCode() : "", s.data);
            writeCell(row, 4,  sub.getCredits() != null ? String.valueOf(sub.getCredits()) : "", s.data);
            writeCell(row, 5,  sub.getDegreeLevel(), s.data);
            writeCell(row, 6,  sub.getTimeAllocation(), s.data);
            writeCell(row, 7,  sub.getMinToPass() != null ? String.valueOf(sub.getMinToPass()) : "", s.data);
            writeCell(row, 8,  sub.getStudentLimit() != null ? String.valueOf(sub.getStudentLimit()) : "", s.data);
            writeCell(row, 9,  sub.getStudentTasks(), s.data);
            writeCell(row, 10, sub.getScoringScale() != null ? String.valueOf(sub.getScoringScale()) : "", s.data);
            writeCell(row, 11, sub.getMinBloomLevel() != null ? String.valueOf(sub.getMinBloomLevel()) : "", s.data);
            writeCell(row, 12, sub.getTheoryPeriods() != null ? String.valueOf(sub.getTheoryPeriods()) : "", s.data);
            writeCell(row, 13, sub.getPracticalPeriods() != null ? String.valueOf(sub.getPracticalPeriods()) : "", s.data);
            writeCell(row, 14, sub.getSelfStudyPeriods() != null ? String.valueOf(sub.getSelfStudyPeriods()) : "", s.data);
        }

        autoSizeColumns(sheet, headers.length);
    }

    // =========================================================
    //  SHEET 4 – CLO_PLO_MAPPING
    //  Columns: Subject Code | Subject Name | CLO Code | Description | Bloom Level | PLO1 | PLO2 ...
    //  Rule: Merge cols 0-1 per subject block; mark "x" + green for CLO-PLO mapping
    // =========================================================

    private void writeCloPloMappingSheet(XSSFWorkbook wb, Styles s, Curriculum curriculum) {
        Sheet sheet = wb.createSheet("CLO_PLO_Mapping");

        List<PLOs> plos = plosRepository.findByCurriculum_CurriculumId(curriculum.getCurriculumId());

        // Build header
        int fixedCols = 5;
        String[] headers = new String[fixedCols + plos.size()];
        headers[0] = "Subject Code";
        headers[1] = "Subject Name";
        headers[2] = "CLO Code";
        headers[3] = "Description";
        headers[4] = "Bloom Level";
        for (int i = 0; i < plos.size(); i++) {
            headers[fixedCols + i] = plos.get(i).getPloCode();
        }
        writeHeaderRow(sheet, s, headers);

        // Collect distinct subjects in curriculum order
        List<Curriculum_Group_Subject> cgsEntries =
                curriculumGroupSubjectRepository.findAllByCurriculumIdOrderBySemester(curriculum.getCurriculumId());

        LinkedHashMap<UUID, Subject> subjectMap = new LinkedHashMap<>();
        for (Curriculum_Group_Subject cgs : cgsEntries) {
            Subject sub = cgs.getSubject();
            subjectMap.putIfAbsent(sub.getSubjectId(), sub);
        }

        // Build PLO index map for fast column lookup
        Map<UUID, Integer> ploIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < plos.size(); i++) {
            ploIndexMap.put(plos.get(i).getPloId(), i);
        }

        int rowIdx = 1;

        for (Subject subject : subjectMap.values()) {
            List<CLOs> clos = closRepository.findBySubject_SubjectId(subject.getSubjectId());
            if (clos.isEmpty()) continue;

            // Load CLO-PLO mappings for this subject in this curriculum
            List<CLO_PLO_Mapping> mappings = cloPloMappingRepository
                    .findMappingsWithDetails(subject.getSubjectId(), curriculum.getCurriculumId());

            // Build a Set<String> "cloId_ploId" for fast lookup
            Set<String> mappingSet = mappings.stream()
                    .map(m -> m.getClo().getCloId() + "_" + m.getPlo().getPloId())
                    .collect(Collectors.toSet());

            int subjectStartRow = rowIdx;

            for (int i = 0; i < clos.size(); i++) {
                CLOs clo = clos.get(i);
                Row row = sheet.createRow(rowIdx++);

                if (i == 0) {
                    // First CLO row – print subject info
                    writeCell(row, 0, subject.getSubjectCode(), s.data);
                    writeCell(row, 1, subject.getSubjectName(), s.data);
                } else {
                    // Subsequent CLO rows – leave subject cols empty (merged later)
                    writeCell(row, 0, "", s.data);
                    writeCell(row, 1, "", s.data);
                }

                writeCell(row, 2, clo.getCloCode(), s.data);
                writeCell(row, 3, clo.getDescription(), s.data);
                writeCell(row, 4, clo.getBloomLevel(), s.data);

                // PLO matrix
                for (int p = 0; p < plos.size(); p++) {
                    String key = clo.getCloId() + "_" + plos.get(p).getPloId();
                    if (mappingSet.contains(key)) {
                        writeCell(row, fixedCols + p, "x", s.greenMatrix);
                    } else {
                        writeCell(row, fixedCols + p, "", s.data);
                    }
                }
            }

            // Merge subject cols (0, 1) vertically for this subject block
            int subjectEndRow = rowIdx - 1;
            if (subjectEndRow > subjectStartRow) {
                sheet.addMergedRegion(new CellRangeAddress(subjectStartRow, subjectEndRow, 0, 0));
                sheet.addMergedRegion(new CellRangeAddress(subjectStartRow, subjectEndRow, 1, 1));
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    // =========================================================
    //  SHEET 5 – SEMESTER MAPPING
    //  Hierarchical rows:
    //    Group header row (bold): GroupCode col = group name, Credits col = total credits
    //    Subject rows: GroupCode empty, then SubjectCode, SubjectName, Credits, Semester, Prerequisite
    //
    //  Groups:
    //    "Required Knowledge Group": subjects with group type = "Group" OR no group
    //    "Elective Knowledge Group": subjects with group type = "Elective"
    //  Subjects sorted by semester within each group
    // =========================================================

    private void writeSemesterMappingSheet(XSSFWorkbook wb, Styles s, Curriculum curriculum) {
        Sheet sheet = wb.createSheet("Semester Mapping");

        String[] headers = {"Group Code", "Subject Code", "Subject Name", "Credits", "Semester", "Prerequisite"};
        writeHeaderRow(sheet, s, headers);

        List<Curriculum_Group_Subject> allEntries =
                curriculumGroupSubjectRepository.findAllByCurriculumIdOrderBySemester(curriculum.getCurriculumId());

        // Collect all subject IDs to batch-load prerequisites
        List<UUID> allSubjectIds = allEntries.stream()
                .map(cgs -> cgs.getSubject().getSubjectId())
                .distinct()
                .collect(Collectors.toList());

        List<Subject_Prerequisite> allPrereqs = prerequisiteRepository.findBySubject_SubjectIdIn(allSubjectIds);
        // Build map: subjectId -> list of prerequisite codes
        Map<UUID, List<String>> prereqMap = new HashMap<>();
        for (Subject_Prerequisite sp : allPrereqs) {
            prereqMap.computeIfAbsent(sp.getSubject().getSubjectId(), k -> new ArrayList<>())
                     .add(sp.getPrerequisiteSubject().getSubjectCode());
        }

        // Partition entries into Required and Elective groups
        List<Curriculum_Group_Subject> requiredEntries = new ArrayList<>();
        List<Curriculum_Group_Subject> electiveEntries = new ArrayList<>();

        for (Curriculum_Group_Subject cgs : allEntries) {
            Group group = cgs.getGroup();
            if (group == null || "Group".equalsIgnoreCase(group.getType())) {
                requiredEntries.add(cgs);
            } else if ("Elective".equalsIgnoreCase(group.getType())) {
                electiveEntries.add(cgs);
            }
        }

        // Already sorted by semester from the query
        int rowIdx = 1;
        rowIdx = writeGroupBlock(sheet, s, prereqMap, "Required Knowledge Group", requiredEntries, rowIdx);
        writeGroupBlock(sheet, s, prereqMap, "Elective Knowledge Group", electiveEntries, rowIdx);

        autoSizeColumns(sheet, headers.length);
    }

    /**
     * Writes a group header row (bold) followed by subject rows.
     * Returns the next available row index.
     */
    private int writeGroupBlock(Sheet sheet, Styles s, Map<UUID, List<String>> prereqMap,
                                String groupLabel, List<Curriculum_Group_Subject> entries, int rowIdx) {
        if (entries.isEmpty()) return rowIdx;

        // Calculate total credits for this group (deduplicate subjects)
        int totalCredits = entries.stream()
                .map(cgs -> cgs.getSubject())
                .filter(sub -> sub.getCredits() != null)
                .collect(Collectors.toMap(
                        Subject::getSubjectId,
                        sub -> sub,
                        (a, b) -> a,
                        LinkedHashMap::new))
                .values().stream()
                .mapToInt(Subject::getCredits)
                .sum();

        // Group header row (bold)
        Row headerRow = sheet.createRow(rowIdx++);
        writeCell(headerRow, 0, groupLabel, s.groupHeader);
        writeCell(headerRow, 1, "", s.groupHeader);
        writeCell(headerRow, 2, "", s.groupHeader);
        writeCell(headerRow, 3, String.valueOf(totalCredits), s.groupHeader);
        writeCell(headerRow, 4, "", s.groupHeader);
        writeCell(headerRow, 5, "", s.groupHeader);

        // Subject rows (sorted by semester, already sorted from query)
        for (Curriculum_Group_Subject cgs : entries) {
            Subject sub = cgs.getSubject();
            String prereqs = prereqMap.getOrDefault(sub.getSubjectId(), Collections.emptyList())
                    .stream().collect(Collectors.joining(", "));

            Row row = sheet.createRow(rowIdx++);
            writeCell(row, 0, "", s.data);  // GroupCode col empty for subjects
            writeCell(row, 1, sub.getSubjectCode(), s.data);
            writeCell(row, 2, sub.getSubjectName(), s.data);
            writeCell(row, 3, sub.getCredits() != null ? String.valueOf(sub.getCredits()) : "", s.data);
            writeCell(row, 4, cgs.getSemester() != null ? String.valueOf(cgs.getSemester()) : "", s.data);
            writeCell(row, 5, prereqs, s.data);
        }

        return rowIdx;
    }

    // =========================================================
    //  HELPERS
    // =========================================================

    private void writeHeaderRow(Sheet sheet, Styles s, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeCell(row, i, headers[i], s.header);
        }
    }

    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            // Add a small padding
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 512, 20000));
        }
    }

    // =========================================================
    //  STYLE HOLDER
    // =========================================================

    /**
     * Pre-built Apache POI cell styles to avoid repeated style creation
     * (XSSFWorkbook has a hard limit of 64000 unique styles).
     */
    private static class Styles {
        final CellStyle header;
        final CellStyle data;
        final CellStyle greenMatrix;
        final CellStyle groupHeader;

        Styles(XSSFWorkbook wb) {
            // ---- Header style ----
            header = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(header);
            header.setWrapText(true);

            // ---- Normal data style ----
            data = wb.createCellStyle();
            data.setVerticalAlignment(VerticalAlignment.TOP);
            setBorders(data);
            data.setWrapText(true);

            // ---- Green matrix style (for "x" marks) ----
            greenMatrix = wb.createCellStyle();
            // Light Green 3 approximation: #CCFFCC  (Excel "Light Green 3" = #C6EFCE-ish)
            XSSFColor lightGreen = new XSSFColor(new byte[]{(byte) 0xC6, (byte) 0xEF, (byte) 0xCE}, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) greenMatrix).setFillForegroundColor(lightGreen);
            greenMatrix.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            greenMatrix.setAlignment(HorizontalAlignment.CENTER);
            greenMatrix.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(greenMatrix);
            Font xFont = wb.createFont();
            xFont.setBold(true);
            greenMatrix.setFont(xFont);

            // ---- Group header style (bold, light yellow background) ----
            groupHeader = wb.createCellStyle();
            Font gFont = wb.createFont();
            gFont.setBold(true);
            gFont.setFontHeightInPoints((short) 11);
            groupHeader.setFont(gFont);
            groupHeader.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            groupHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            groupHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(groupHeader);
        }

        private void setBorders(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
