package com.example.smd.controller;

import com.example.smd.dto.request.curriculum.CurriculumCreateRequest;
import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.dto.response.CurriculumShortResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.curriculum.ImportCurriculumResponse;
import com.example.smd.dto.response.curriculum.ImportFullCurriculumResponse;
import com.example.smd.services.CurriculumExcelExportService;
import com.example.smd.services.CurriculumService;
import com.example.smd.services.FullImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Curriculum", description = "Curriculum Management APIs - Quản lý khung chương trình đào tạo")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

        CurriculumService curriculumService;
        FullImportService fullImportService;
        CurriculumExcelExportService curriculumExcelExportService;

        /**
         * API lấy danh sách curriculum với phân trang và bộ lọc
         */
        @GetMapping
        @Operation(summary = "Get curriculums with pagination and filters", description = "Retrieve a paginated list of curriculums. You can filter by curriculum code, name, major, and status.")
        public ResponseObject<PagedResponse<CurriculumResponse>> getAllCurriculums(
                        @Parameter(description = "Search keyword for curriculum code or name") @RequestParam(required = false) String search,

                        @Parameter(description = "Search by: 'code', 'name', or 'all'") @RequestParam(required = false) String searchBy,

                        @Parameter(description = "Bộ lọc trạng thái vòng đời của Khung chương trình (Curriculum Lifecycle). Các giá trị hợp lệ bao gồm:\n\n"
                                        +
                                        "| Status | Ý nghĩa nghiệp vụ (Nghiệp vụ) |\n" +
                                        "| :--- | :--- |\n" +
                                        "| **DRAFT** | **Biên soạn nháp:** HoCF đang khởi tạo cấu trúc khung, chưa hiển thị cho các vai trò khác. |\n"
                                        +
                                        "| **STRUCTURE_REVIEWED** | **Đang duyệt cấu trúc:** Đang trong quá trình đợi Vice President (VP) xem xét và đánh giá khung. |\n"
                                        +
                                        "| **STRUCTURE_APPROVED** | **Đã duyệt cấu trúc:** VP đã ký duyệt khung, **chính thức bàn giao** nhiệm vụ cho HoPDC. |\n"
                                        +
                                        "| **SYLLABUS_DEVELOP**| **Phát triển Syllabus:** HoPDC và các Bộ môn đang xây dựng chi tiết từng đề cương môn học. |\n"
                                        +
                                        "| **FINAL_REVIEW** | **Thẩm định cuối:** Hội đồng rà soát toàn bộ nội dung (Khung + Syllabus) trước khi trình ký chính thức. |\n"
                                        +
                                        "| **SIGNED** | **Đã ký ban hành:** Đã có quyết định ban hành chính thức từ Ban giám hiệu/VP. |\n"
                                        +
                                        "| **PUBLISHED** | **Công bố:** Chương trình và tất cả Syllabus liên kết đã công khai cho sinh viên tra cứu. |\n"
                                        +
                                        "| **ARCHIVED** | **Lưu trữ:** Phiên bản cũ, không còn áp dụng cho các khóa tuyển sinh mới. |") @RequestParam(required = false) String status,

                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,

                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,

                        @Parameter(description = "Sort field and direction: [field, asc|desc]") @RequestParam(defaultValue = "curriculumCode,asc") String[] sort,

                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                Page<CurriculumResponse> curriculums = curriculumService.getAllCurriculums(
                                search, searchBy, status, page, size, sort, userId);

                return ResponseObject.<PagedResponse<CurriculumResponse>>builder()
                                .status(1000)
                                .data(PagedResponse.of(curriculums))
                                .message("Get all curriculums successfully")
                                .build();
        }

        /**
         * API tạo curriculum mới
         */
        @PostMapping
        @PreAuthorize("hasAuthority('CURRICULUM_CREATE')")
        @Operation(summary = "Create a new curriculum", description = "Create a new curriculum with unique curriculum code. Requires CURRICULUM_CREATE permission.")
        public ResponseObject<CurriculumResponse> createCurriculum(
                        @RequestBody @Valid CurriculumCreateRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.createCurriculum(request, userId))
                                .message("Curriculum created successfully")
                                .build();
        }

        /**
         * API lấy chi tiết curriculum theo ID
         */
        @GetMapping("/{id}")
        @Operation(summary = "Get curriculum detail by ID", description = "Retrieve full curriculum details including major information.")
        public ResponseObject<CurriculumResponse> getCurriculumDetail(
                        @Parameter(description = "Curriculum ID (UUID)") @PathVariable String id,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.getCurriculumDetail(id, userId))
                                .message("Get curriculum detail successfully")
                                .build();
        }

        /**
         * API lấy danh sách curriculum theo Major ID
         */
        @GetMapping("/major/{majorId}")
        @Operation(summary = "Get curriculums by Major ID", description = "Retrieve a list of curriculums associated with a specific major. Returns basic curriculum info without major details.")
        public ResponseObject<List<CurriculumShortResponse>> getCurriculumsByMajor(
                        @Parameter(description = "Major ID (UUID)") @PathVariable UUID majorId) {
                return ResponseObject.<List<CurriculumShortResponse>>builder()
                                .status(1000)
                                .data(curriculumService.getCurriculumsByMajor(majorId))
                                .message("Get curriculums by major successfully")
                                .build();
        }

        /**
         * API lấy curriculum theo code
         */
        @GetMapping("/code/{code}")
        @Operation(summary = "Get curriculum by curriculum code", description = "Retrieve curriculum information using its unique code.")
        public ResponseObject<CurriculumResponse> getCurriculumByCode(
                        @Parameter(description = "Curriculum code") @PathVariable String code,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.getCurriculumByCode(code, userId))
                                .message("Get curriculum by code successfully")
                                .build();
        }

        /**
         * API cập nhật curriculum
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
        @Operation(summary = "Update an existing curriculum", description = "Update curriculum details. Requires CURRICULUM_UPDATE permission.")
        public ResponseObject<CurriculumResponse> updateCurriculum(
                        @Parameter(description = "Curriculum ID (UUID)") @PathVariable String id,
                        @RequestBody @Valid CurriculumCreateRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.updateCurriculum(id, request, userId))
                                .message("Curriculum updated successfully")
                                .build();
        }

        /**
         * API cập nhật status của curriculum
         */
        @PatchMapping("/{id}/status")
        @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
        @Operation(summary = "Update Curriculum Life-cycle Status", description = "### Curriculum Life-cycle Workflow (Quy trình vòng đời Khung chương trình):\n"
                        +
                        "Select one of the following values to coordinate the approval and syllabus development flow:\n\n"
                        +
                        "| Status | Business Logic Description (Mô tả nghiệp vụ) |\n" +
                        "| :--- | :--- |\n" +
                        "| **DRAFT** | **Khởi tạo:** HoC/FDC đang thiết kế cấu trúc khung và danh mục môn học. Chỉ hiển thị nội bộ, cho phép chỉnh sửa toàn diện. |\n"
                        +
                        "| **STRUCTURE_REVIEWED** | **Đã duyệt cấu trúc:** Văn phòng (VP) đã duyệt danh mục môn học. Chờ quyết định hành chính để triển khai tiếp. |\n"
                        +
                        "| **SYLLABUS_DEVELOP** | **Soạn thảo Syllabus:** Hệ thống mở quyền cho các Bộ môn bắt đầu biên soạn Syllabus chi tiết dựa trên khung đã duyệt. |\n"
                        +
                        "| **FINAL_REVIEW** | **Rà soát tổng thể:** HoC/FDC kiểm tra lại toàn bộ nội dung Syllabus và Khung trước khi trình ký chính thức. |\n"
                        +
                        "| **SIGNED** | **Đã ký ban hành:** VP đã ký quyết định phê duyệt. Dữ liệu được khóa để chuẩn bị xuất bản. |\n"
                        +
                        "| **PUBLISHED** | **Đã xuất bản:** Khung chương trình chính thức áp dụng cho khóa tuyển sinh. Cho phép mọi người dùng xem (Public view). |\n"
                        +
                        "| **ARCHIVED** | **Lưu trữ:** Khung cũ đã hết hiệu lực. Chuyển sang chế độ chỉ đọc (Read-only) để tra cứu lịch sử đào tạo. |")
        public ResponseObject<CurriculumResponse> updateCurriculumStatus(
                        @Parameter(description = "Curriculum ID (UUID)") @PathVariable String id,
                        @Parameter(description = "New status: DRAFT, INTERNAL_REVIEW_WITHOUT_ENACTMENT, INTERNAL_REVIEW_WITH_ENACTMENT, PUBLISHED, ARCHIVED") @RequestParam String status) {
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.updateCurriculumStatus(id, status))
                                .message("Curriculum status updated successfully")
                                .build();
        }

        /**
         * API đồng bộ status của Curriculum, PLOs, Major, POs, Subjects
         */
        @PatchMapping("/{id}/sync-status")
//        @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
        @Operation(summary = "Dùng khi mới major và curriculum",
                description =
                "Updates the curriculum status to SYLLABUS_DEVELOP " +
                        "and synchronizes Curriculum, PLOs, Major, POs, and Subjects statuses.")
        public ResponseObject<Void> syncCurriculumStatus(
                        @Parameter(description = "Curriculum ID (UUID)") @PathVariable String id,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                curriculumService.syncCurriculumStatus(id, userId);
                return ResponseObject.<Void>builder()
                                .status(1000)
                                .message("Curriculum and related entities statuses synchronized successfully")
                                .build();
        }

        /**
         * API cập nhật endYear của curriculum
         */
        @PatchMapping("/{id}/end-year")
        @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
        public ResponseObject<CurriculumResponse> updateCurriculumEndYear(
                        @Parameter(description = "Curriculum ID (UUID)") @PathVariable String id,

                        @Parameter(description = "New end-year must be greater than or equal to start-year") @RequestParam int endYear,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<CurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.updateCurriculumEndYear(id, endYear, userId))
                                .message("Curriculum end-year updated successfully")
                                .build();
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('CURRICULUM_DELETE')")
        @Operation(summary = "Soft delete curriculum", description = "Sets curriculum status to inactive instead of hard deleting from database")
        public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                curriculumService.delete(id, userId);
                return ResponseObject.<Void>builder()
                                .message("Curriculum deleted successfully")
                                .build();
        }

        @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('CURRICULUM_CREATE')")
        @Operation(
                summary = "Import Curriculums from Excel",
                description = "Import Curriculum records (with PLOs) from an Excel file.\n\n" +
                        "**Sheet name:** `Curriculum`\n\n" +
                        "**Column headers (row 1):**\n" +
                        "| Column | Required | Description |\n" +
                        "| :--- | :--- | :--- |\n" +
                        "| **Curriculum Code** | Yes | Unique code for the curriculum (e.g. SE1901) |\n" +
                        "| **Name** | Yes | Full name of the curriculum |\n" +
                        "| **Start Year** | No | Year the curriculum starts (e.g. 2019) |\n" +
                        "| **Description** | No | Description of the curriculum |\n" +
                        "| **Major Code** | Yes | Existing Major Code the curriculum belongs to |\n" +
                        "| **PLO Code** | Yes | Unique PLO code (e.g. PLO1, PLO2). Each row = one PLO. |\n" +
                        "| **PLO Description** | No | Description of the PLO |\n\n" +
                        "**Notes:**\n" +
                        "- One Curriculum can have multiple PLOs — repeat the Curriculum Code on each row.\n" +
                        "- If a Curriculum Code already exists in the system, all rows for that Curriculum will be rejected.\n" +
                        "- PLO Code must be globally unique across all Curriculums.\n" +
                        "- Major Code must already exist in the system."
        )
        public ResponseObject<ImportCurriculumResponse> importCurriculums(
                @RequestParam("file") MultipartFile file
        ) {
                return ResponseObject.<ImportCurriculumResponse>builder()
                                .status(1000)
                                .data(curriculumService.importCurriculums(file))
                                .message("Import curriculums completed")
                                .build();
        }

        @PostMapping(value = "/import-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('CURRICULUM_CREATE')")
        @Operation(
                summary = "Import Full Curriculum from Excel",
                description = "Imports an entire curriculum system from a single Excel file with multiple sheets.\n\n" +
                        "**Expected Sheets:**\n" +
                        "- `Major`: Major data and POs.\n" +
                        "- `Curriculum`: Curriculum data, PLOs, and PO mapping.\n" +
                        "- `Subject`: Subjects list.\n" +
                        "- `Group`: Elective groups.\n" +
                        "- `Semester Mapping`: Mapping between Curriculum, Group, Subject, and Semester.\n"+
                        "- `Source`"
        )
        public ResponseObject<ImportFullCurriculumResponse> importFullCurriculum(
                @RequestParam("file") MultipartFile file
        ) {
                return ResponseObject.<ImportFullCurriculumResponse>builder()
                                .status(1000)
                                .data(fullImportService.importFullCurriculum(file))
                                .message("Full curriculum import completed")
                                .build();
        }

        /**
         * API Export toàn bộ Curriculum ra file Excel 5 sheets
         */
        @GetMapping("/{id}/export")
        @Operation(
                summary = "Export Full Curriculum to Excel",
                description = "Exports the full curriculum data (Major, Curriculum, Subjects, CLO-PLO mapping, "
                        + "Semester mapping) into a formatted 5-sheet Excel report file (.xlsx).")
        public ResponseEntity<InputStreamResource> exportCurriculum(
                @Parameter(description = "Curriculum ID (UUID)") @PathVariable UUID id,
                 @AuthenticationPrincipal Jwt jwt
        ) {
                String userId = jwt.getClaimAsString("accountId");

                ByteArrayInputStream excelStream = curriculumExcelExportService.exportFullCurriculum(id);
                var curriculum =
                        curriculumService.getCurriculumDetail(id.toString(), userId);
                String fileName =curriculum.getCurriculumCode()+
                        "-" + curriculum.getCurriculumName();
                String finalFileName = fileName + ".xlsx";

                ContentDisposition contentDisposition = ContentDisposition
                        .attachment()
                        .filename(finalFileName, StandardCharsets.UTF_8)
                        .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentDisposition(contentDisposition);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelStream));
        }
}
