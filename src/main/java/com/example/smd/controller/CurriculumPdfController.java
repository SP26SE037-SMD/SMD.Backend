package com.example.smd.controller;

import com.example.smd.services.CurriculumPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/curricula")
@RequiredArgsConstructor
@Tag(name = "Curriculum PDF", description = "Export full curriculum to PDF")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumPdfController {

    private final CurriculumPdfService curriculumPdfService;

    /**
     * GET /api/curricula/{curriculumId}/export-pdf
     * Returns a downloadable PDF file of the full curriculum report.
     */
    @GetMapping("/{curriculumId}/export-pdf")
    @Operation(summary = "Export full curriculum report as PDF")
    public ResponseEntity<InputStreamResource> exportPdf(@PathVariable UUID curriculumId) {

        ByteArrayInputStream pdfStream = curriculumPdfService.exportPdf(curriculumId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"curriculum-" + curriculumId + ".pdf\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
