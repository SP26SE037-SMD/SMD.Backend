package com.example.smd.dto.response.pdf;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

/**
 * Aggregates all data sections required to render the full-curriculum PDF.
 * Passed as Thymeleaf context variable "data".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumPdfData {

    // ── Page 1: Cover / Major info ────────────────────────────────────────────
    MajorSection major;

    // ── Page 2: PLO table + PO-PLO matrix ────────────────────────────────────
    CurriculumSection curriculum;

    // ── Page 3: Semester teaching plan ───────────────────────────────────────
    List<SemesterGroup> semesterPlan;

    // ── Page 4: Subject detail cards ─────────────────────────────────────────
    List<SubjectCard> subjects;

    // ── Page 5: CLO-PLO matrix (landscape) ───────────────────────────────────
    CloPloMatrix cloPloMatrix;

    // ══════════════════════ Inner DTOs ════════════════════════════════════════

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MajorSection {
        String majorCode;
        String majorName;
        String description;
        List<PoRow> pos;          // Program Objectives

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PoRow {
            String poCode;
            String description;
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CurriculumSection {
        String curriculumCode;
        String curriculumName;
        Integer startYear;
        Integer endYear;
        List<PloRow> plos;
        /** poHeaders: ordered list of PO codes for matrix columns */
        List<String> poHeaders;
        /** matrix: ploCode -> Set<poCode> that are mapped */
        Map<String, Set<String>> ploPoMappings;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PloRow {
            String ploCode;
            String description;
            String status;
        }
    }

    /** One group (khối kiến thức) in the semester plan */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SemesterGroup {
        String groupCode;
        String groupName;
        String groupType;           // Mandatory / Elective
        int totalCredits;
        List<SemesterSubjectRow> rows;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class SemesterSubjectRow {
            Integer semester;
            String subjectCode;
            String subjectName;
            Integer credits;
            String degreeLevel;
        }
    }

    /** Rich card for one subject (Page 4) */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectCard {
        String subjectCode;
        String subjectName;
        Integer credits;
        Integer theoryPeriods;
        Integer practicalPeriods;
        Integer selfStudyPeriods;
        String degreeLevel;
        String timeAllocation;
        String description;
        String tool;
        Integer minBloomLevel;
        Integer scoringScale;
        Integer minToPass;
        String departmentName;
        String status;
    }

    /** CLO-PLO matrix data (Page 5, landscape) */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CloPloMatrix {
        /** PLO header codes (columns) */
        List<String> ploHeaders;
        /** Rows grouped by subject */
        List<SubjectCloGroup> subjectGroups;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class SubjectCloGroup {
            String subjectCode;
            String subjectName;
            int cloCount;           // for rowspan
            List<CloRow> cloRows;

            @Data @Builder @NoArgsConstructor @AllArgsConstructor
            public static class CloRow {
                String cloCode;
                String cloName;
                /** ploCode -> true if mapped */
                Map<String, Boolean> ploMapping;
            }
        }
    }
}
