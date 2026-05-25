package com.example.smd.controller;

import com.example.smd.services.CurriculumPdfService;
import com.example.smd.services.CurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/curricula")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Curriculum PDF", description = "Export full curriculum to PDF")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumPdfController {
    private CurriculumService curriculumService;
    private final CurriculumPdfService curriculumPdfService;

    /**
     * GET /api/curricula/{curriculumId}/export-pdf
     * Returns a downloadable PDF file of the full curriculum report.
     */
    @GetMapping("/{curriculumId}/export-pdf")
    @Operation(summary = "Export full curriculum report as PDF")
    public ResponseEntity<InputStreamResource> exportPdf(
            @PathVariable UUID curriculumId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        ByteArrayInputStream pdfStream = curriculumPdfService.exportPdf(curriculumId);
        String userId = jwt.getClaimAsString("accountId");
        var curriculum =
                curriculumService.getCurriculumDetail(curriculumId.toString(), userId);
        String fileName =curriculum.getCurriculumCode()+
                "-" + curriculum.getCurriculumName();
        String finalFileName = fileName + ".pdf";

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(finalFileName, StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
//        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"curriculum-" + curriculumId + ".pdf\"");
//        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
