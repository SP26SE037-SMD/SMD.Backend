package com.example.smd.services;

import com.example.smd.dto.response.pdf.CurriculumPdfData;
import com.example.smd.dto.response.pdf.CurriculumPdfData.*;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembles all data from the database, renders the Thymeleaf template
 * to an HTML string, then converts that HTML to a PDF byte stream using
 * OpenHTMLToPDF.
 *
 * Usage:
 *   byte[] pdf = curriculumPdfService.exportPdf(curriculumId).readAllBytes();
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumPdfService {

    // ── Repositories ──────────────────────────────────────────────────────────
    private final CurriculumRepository              curriculumRepository;
    private final MajorRepository                   majorRepository;
    private final POsRepository                     posRepository;
    private final PLOsRepository                    plosRepository;
    private final PoPloMappingRepository            poPloMappingRepository;
    private final CurriculumGroupSubjectRepository  cgsRepository;
    private final CLOsRepository                    closRepository;
    private final CloPloMappingRepository           cloPloMappingRepository;

    // ── Thymeleaf ─────────────────────────────────────────────────────────────
    private final TemplateEngine templateEngine;

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportPdf(UUID curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        UUID majorId = curriculum.getMajor().getMajorId();

        CurriculumPdfData data = CurriculumPdfData.builder()
                .major(buildMajorSection(majorId))
                .curriculum(buildCurriculumSection(curriculum))
                .semesterPlan(buildSemesterPlan(curriculumId))
                .subjects(buildSubjectCards(curriculumId))
                .cloPloMatrix(buildCloPloMatrix(curriculumId))
                .build();

        String html = renderTemplate(data);
        return convertHtmlToPdf(html);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

    // ── Page 1: Major & POs ───────────────────────────────────────────────────

    private MajorSection buildMajorSection(UUID majorId) {
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        List<MajorSection.PoRow> poRows = posRepository.findByMajor_MajorId(majorId)
                .stream()
                .sorted(Comparator.comparing(PO::getPoCode))
                .map(po -> MajorSection.PoRow.builder()
                        .poCode(po.getPoCode())
                        .description(po.getDescription())
                        .build())
                .collect(Collectors.toList());

        return MajorSection.builder()
                .majorCode(major.getMajorCode())
                .majorName(major.getMajorName())
                .description(major.getDescription())
                .pos(poRows)
                .build();
    }

    // ── Page 2: Curriculum, PLOs + PO-PLO matrix ──────────────────────────────

    private CurriculumSection buildCurriculumSection(Curriculum curriculum) {
        UUID curriculumId = curriculum.getCurriculumId();
        UUID majorId      = curriculum.getMajor().getMajorId();

        // PLOs
        List<PLOs> ploList = plosRepository.findByCurriculum_CurriculumId(curriculumId)
                .stream()
                .sorted(Comparator.comparing(PLOs::getPloCode))
                .collect(Collectors.toList());

        List<CurriculumSection.PloRow> ploRows = ploList.stream()
                .map(p -> CurriculumSection.PloRow.builder()
                        .ploCode(p.getPloCode())
                        .description(p.getDescription())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());

        // POs (columns)
        List<PO> poList = posRepository.findByMajor_MajorId(majorId)
                .stream()
                .sorted(Comparator.comparing(PO::getPoCode))
                .collect(Collectors.toList());
        List<String> poHeaders = poList.stream().map(PO::getPoCode).collect(Collectors.toList());

        // PO-PLO mapping matrix  (scoped to this curriculum)
        List<PO_PLO_Mapping> mappings =
                poPloMappingRepository.findByPlo_Curriculum_CurriculumId(curriculumId);
        Map<UUID, String> ploIdToCode = ploList.stream()
                .collect(Collectors.toMap(PLOs::getPloId, PLOs::getPloCode));
        Map<UUID, String> poIdToCode = poList.stream()
                .collect(Collectors.toMap(PO::getPoId, PO::getPoCode));

        // ploCode -> Set<poCode>
        Map<String, Set<String>> ploPoMappings = new LinkedHashMap<>();
        ploRows.forEach(r -> ploPoMappings.put(r.getPloCode(), new LinkedHashSet<>()));

        mappings.forEach(m -> {
                    String ploCode = ploIdToCode.get(m.getPlo().getPloId());
                    String poCode  = poIdToCode.get(m.getPo().getPoId());
                    if (ploCode != null && poCode != null) {
                        ploPoMappings.get(ploCode).add(poCode);
                    }
                });

        return CurriculumSection.builder()
                .curriculumCode(curriculum.getCurriculumCode())
                .curriculumName(curriculum.getCurriculumName())
                .startYear(curriculum.getStartYear())
                .endYear(curriculum.getEndYear())
                .plos(ploRows)
                .poHeaders(poHeaders)
                .ploPoMappings(ploPoMappings)
                .build();
    }

    // ── Page 3: Semester plan (grouped by knowledge block / Group) ─────────────

    private List<SemesterGroup> buildSemesterPlan(UUID curriculumId) {
        List<Curriculum_Group_Subject> cgsList =
                cgsRepository.findAllByCurriculumIdOrderBySemester(curriculumId);

        // Group by Group entity (null group → "Uncategorized")
        Map<UUID, List<Curriculum_Group_Subject>> byGroup = new LinkedHashMap<>();
        Map<UUID, Group> groupMap = new LinkedHashMap<>();

        for (Curriculum_Group_Subject cgs : cgsList) {
            UUID key = cgs.getGroup() != null ? cgs.getGroup().getGroupId() : null;
            byGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(cgs);
            if (cgs.getGroup() != null) groupMap.put(key, cgs.getGroup());
        }

        List<SemesterGroup> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Curriculum_Group_Subject>> entry : byGroup.entrySet()) {
            UUID    gId  = entry.getKey();
            Group   grp  = groupMap.get(gId);
            List<Curriculum_Group_Subject> rows = entry.getValue();

            int totalCredits = rows.stream()
                    .mapToInt(r -> r.getSubject() != null && r.getSubject().getCredits() != null
                            ? r.getSubject().getCredits() : 0)
                    .sum();

            List<SemesterGroup.SemesterSubjectRow> subjectRows = rows.stream()
                    .map(r -> {
                        Subject s = r.getSubject();
                        return SemesterGroup.SemesterSubjectRow.builder()
                                .semester(r.getSemester())
                                .subjectCode(s != null ? s.getSubjectCode() : "")
                                .subjectName(s != null ? s.getSubjectName() : "")
                                .credits(s != null ? s.getCredits() : 0)
                                .degreeLevel(s != null ? s.getDegreeLevel() : "")
                                .build();
                    })
                    .collect(Collectors.toList());

            result.add(SemesterGroup.builder()
                    .groupCode(grp != null ? grp.getGroupCode() : "UNCATEGORIZED")
                    .groupName(grp != null ? grp.getGroupName() : "Uncategorized")
                    .groupType(grp != null ? grp.getType() : "")
                    .totalCredits(totalCredits)
                    .rows(subjectRows)
                    .build());
        }
        return result;
    }

    // ── Page 4: Subject detail cards ──────────────────────────────────────────

    private List<SubjectCard> buildSubjectCards(UUID curriculumId) {
        return cgsRepository.findAllByCurriculumIdOrderBySemester(curriculumId)
                .stream()
                .map(Curriculum_Group_Subject::getSubject)
                .filter(Objects::nonNull)
                .distinct()
                .map(s -> SubjectCard.builder()
                        .subjectCode(s.getSubjectCode())
                        .subjectName(s.getSubjectName())
                        .credits(s.getCredits())
                        .theoryPeriods(s.getTheoryPeriods())
                        .practicalPeriods(s.getPracticalPeriods())
                        .selfStudyPeriods(s.getSelfStudyPeriods())
                        .degreeLevel(s.getDegreeLevel())
                        .timeAllocation(s.getTimeAllocation())
                        .description(s.getDescription())
                        .tool(s.getTool())
                        .minBloomLevel(s.getMinBloomLevel())
                        .scoringScale(s.getScoringScale())
                        .minToPass(s.getMinToPass())
                        .departmentName(s.getDepartment() != null
                                ? s.getDepartment().getDepartmentName() : "")
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Page 5: CLO-PLO matrix ────────────────────────────────────────────────

//    private CloPloMatrix buildCloPloMatrix(UUID curriculumId) {
//        // PLOs (column headers)
//        List<PLOs> ploList = plosRepository.findByCurriculum_CurriculumId(curriculumId)
//                .stream()
//                .sorted(Comparator.comparing(PLOs::getPloCode))
//                .collect(Collectors.toList());
//        List<String> ploHeaders = ploList.stream().map(PLOs::getPloCode).collect(Collectors.toList());
//        Map<UUID, String> ploIdToCode = ploList.stream()
//                .collect(Collectors.toMap(PLOs::getPloId, PLOs::getPloCode));
//
//        // Subjects in this curriculum (ordered by semester)
//        List<Subject> subjects = cgsRepository.findAllByCurriculumIdOrderBySemester(curriculumId)
//                .stream()
//                .map(Curriculum_Group_Subject::getSubject)
//                .filter(Objects::nonNull)
//                .distinct()
//                .collect(Collectors.toList());
//
//        List<CloPloMatrix.SubjectCloGroup> subjectGroups = new ArrayList<>();
//
//        for (Subject subject : subjects) {
//            // CLOs of this subject
//            List<CLOs> cloList = closRepository.findBySubject_SubjectId(subject.getSubjectId())
//                    .stream()
//                    .sorted(Comparator.comparing(CLOs::getCloCode))
//                    .collect(Collectors.toList());
//
//            if (cloList.isEmpty()) continue;
//
//            List<CloPloMatrix.SubjectCloGroup.CloRow> cloRows = new ArrayList<>();
//            for (CLOs clo : cloList) {
//                // Which PLOs does this CLO map to?
//                Set<String> mappedPloCodes = cloPloMappingRepository
//                        .findByClo_CloId(clo.getCloId())
//                        .stream()
//                        .map(m -> ploIdToCode.get(m.getPlo().getPloId()))
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toSet());
//
//                // Build boolean map: ploCode -> marked?
//                Map<String, Boolean> ploMapping = new LinkedHashMap<>();
//                ploHeaders.forEach(code -> ploMapping.put(code, mappedPloCodes.contains(code)));
//
//                cloRows.add(CloPloMatrix.SubjectCloGroup.CloRow.builder()
//                        .cloCode(clo.getCloCode())
//                        .cloName(clo.getDescription())
//                        .ploMapping(ploMapping)
//                        .build());
//            }
//
//            subjectGroups.add(CloPloMatrix.SubjectCloGroup.builder()
//                    .subjectCode(subject.getSubjectCode())
//                    .subjectName(subject.getSubjectName())
//                    .cloCount(cloRows.size())
//                    .cloRows(cloRows)
//                    .build());
//        }
//
//        return CloPloMatrix.builder()
//                .ploHeaders(ploHeaders)
//                .subjectGroups(subjectGroups)
//                .build();
//    }

    private CloPloMatrix buildCloPloMatrix(UUID curriculumId) {
        // Lấy danh sách PLO từ DB
        List<PLOs> rawPloList = plosRepository.findByCurriculum_CurriculumId(curriculumId);

        // FIX 1: Lọc trùng lặp (distinct) và Sắp xếp tự nhiên (Natural Sort: PLO1 -> PLO2 -> PLO10)
        List<String> ploHeaders = rawPloList.stream()
                .map(PLOs::getPloCode)
                .filter(Objects::nonNull)
                .distinct() // Lọc bỏ các PLO bị lặp lại
                .sorted(Comparator.comparingInt(code -> {
                    // Tách lấy phần số để sắp xếp (Ví dụ: "PLO12" -> 12)
                    String num = code.replaceAll("\\D+", "");
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }))
                .collect(Collectors.toList());

        // Subjects in this curriculum (ordered by semester)
        List<Subject> subjects = cgsRepository.findAllByCurriculumIdOrderBySemester(curriculumId)
                .stream()
                .map(Curriculum_Group_Subject::getSubject)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<CloPloMatrix.SubjectCloGroup> subjectGroups = new ArrayList<>();

        for (Subject subject : subjects) {
            // CLOs of this subject
            List<CLOs> cloList = closRepository.findBySubject_SubjectId(subject.getSubjectId())
                    .stream()
                    // Sắp xếp CLO theo số thứ tự tự nhiên (CLO1 -> CLO2 -> CLO10)
                    .sorted(Comparator.comparingInt(c -> {
                        String num = c.getCloCode().replaceAll("\\D+", "");
                        return num.isEmpty() ? 0 : Integer.parseInt(num);
                    }))
                    .collect(Collectors.toList());

            if (cloList.isEmpty()) continue;

            List<CloPloMatrix.SubjectCloGroup.CloRow> cloRows = new ArrayList<>();

            for (CLOs clo : cloList) {
                // FIX 2: Lấy TRỰC TIẾP mã Code từ DB, không qua Map ID trung gian để tránh lỗi Null
                Set<String> mappedPloCodes = cloPloMappingRepository
                        .findByClo_CloId(clo.getCloId())
                        .stream()
                        .map(m -> m.getPlo().getPloCode()) // Lấy thẳng mã PLO (Ví dụ: "PLO1")
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // Build boolean map: ploCode -> marked?
                Map<String, Boolean> ploMapping = new LinkedHashMap<>();
                ploHeaders.forEach(code -> ploMapping.put(code, mappedPloCodes.contains(code)));

                cloRows.add(CloPloMatrix.SubjectCloGroup.CloRow.builder()
                        .cloCode(clo.getCloCode())
                        .cloName(clo.getDescription())
                        .ploMapping(ploMapping)
                        .build());
            }

            subjectGroups.add(CloPloMatrix.SubjectCloGroup.builder()
                    .subjectCode(subject.getSubjectCode())
                    .subjectName(subject.getSubjectName())
                    .cloCount(cloRows.size())
                    .cloRows(cloRows)
                    .build());
        }

        return CloPloMatrix.builder()
                .ploHeaders(ploHeaders)
                .subjectGroups(subjectGroups)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE + PDF RENDERING
    // ═════════════════════════════════════════════════════════════════════════

    private String renderTemplate(CurriculumPdfData data) {
        Context ctx = new Context();
        ctx.setVariable("data", data);
        return templateEngine.process("pdf/curriculum-export", ctx);
    }

    private ByteArrayInputStream convertHtmlToPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
