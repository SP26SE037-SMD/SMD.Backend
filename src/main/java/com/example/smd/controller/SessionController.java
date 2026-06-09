package com.example.smd.controller;

import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.dto.response.validate.SessionImportResult;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.services.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@Tag(name = "Session", description = "Session Management APIs")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class  SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "Get all sessions with pagination and filters")
    public ResponseObject<PagedResponse<SessionResponse>> getAllSessions(
            @RequestParam(required = false) UUID syllabusId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sessionNumber,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<SessionResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(sessionService.getAllSessions(
                        syllabusId,  search, page, size, sort
                )))
                .message("Get all sessions successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    public ResponseObject<SessionResponse> getSessionById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.getSessionById(id, userId))
                .message("Get session successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all sessions by syllabus")
    public ResponseObject<List<SessionResponse>> getSessionsBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<SessionResponse>>builder()
                .status(1000)
                .data(sessionService.getSessionsBySyllabus(syllabusId))
                .message("Get sessions by syllabus successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new session")
    public ResponseObject<SessionResponse> createSession(@Valid @RequestBody SessionRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.createSession(request, userId))
                .message("Create session successfully")
                .build();
    }

    @PostMapping("/bluk")
//    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new session")
    public ResponseObject<List<SessionResponse>> createSessionBluk(@Valid @RequestBody List<SessionRequest> request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<SessionResponse>>builder()
                .status(1000)
                .data(sessionService.createSessionsBluk(request, userId))
                .message("Create session successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update session by ID")
    public ResponseObject<SessionResponse> updateSession(
            @PathVariable UUID id,
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.updateSession(id, request, userId))
                .message("Update session successfully")
                .build();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete session by status rule")
    public ResponseObject<Boolean> deleteSession(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(sessionService.deleteSession(id, userId))
                .message("Delete session successfully")
                .build();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete session list by syllabus and session numbers")
    public ResponseObject<Boolean> deleteSessionList(
            @RequestParam UUID syllabusId,
            @Valid @RequestBody SessionNumberListRequest request
    ) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(sessionService.deleteSessionListBySyllabusAndSessionNumbers(syllabusId, request))
                .message("Delete session list successfully")
                .build();
    }


    @PostMapping("/syllabus/{syllabusId}/validate")
    @Operation(
            summary = "Kiểm tra tính hợp lệ của quỹ thời gian Sessions",
            description = "Hàm này tính toán và đối chiếu quỹ thời gian dựa trên danh sách Session truyền vào và các Session đã có sẵn trong Database. " +
                    "Logic xử lý được phân loại chi tiết theo từng `sessionType`:\n\n" +
                    "* **THEORY (Lý thuyết)**: Tổng thời lượng (giờ) được tính toán, làm tròn và quy đổi sang tiết học (45 phút = 1 tiết). Sau đó đối chiếu với quỹ tiết lý thuyết của Subject.\n" +
                    "* **PRACTICE (Thực hành)**: Tương tự như Theory, tổng giờ thực hành được quy đổi sang tiết học và đối chiếu với quỹ tiết thực hành của Subject.\n" +
                    "* **SELF_STUDY (Tự học)**: Tổng thời lượng (giờ) được cộng dồn trực tiếp và đối chiếu với quỹ giờ tự học quy định.\n\n" +
                    "**Kết quả trả về:** Bao gồm chi tiết số dư quỹ thời gian (`remainingQuotas`) và danh sách các mã lỗi cảnh báo (VD: `THEORY_SHORTAGE`, `PRACTICE_SURPLUS`, `SELF_STUDY_SHORTAGE`...) nếu phát hiện sự phân bổ THIẾU hoặc DƯ so với quy định."
    )
    public ResponseObject<SessionValidationResult> validateSession(
            @RequestBody List<SessionRequest> inputs,
            @PathVariable("syllabusId") UUID syllabusId) {
        return ResponseObject.<SessionValidationResult>builder()
                .status(1000)
                .data(sessionService.validate(inputs, syllabusId))
                .message("Validate session successfully")
                .build();
    }

    // ------------------------------------------------------------------ //
    //                      EXPORT TO EXCEL                               //
    // ------------------------------------------------------------------ //

    @GetMapping("/syllabus/{syllabusId}/export")
    @Operation(
            summary = "Export danh sách Session ra file Excel",
            description = """
                    Xuất toàn bộ Session của một Syllabus ra file .xlsx có cấu trúc cột
                    hoàn toàn giống với template Import:

                    | Cột 0           | Cột 1 | Cột 2           | Cột 3 | Cột 4 | Cột 5       |
                    |---|---|---|---|---|---|
                    | Session Number | Title | Teaching Methods | Topic | Type | CLO-Mapping |

                    - **Topic**: ký tự xuống dòng (Alt+Enter) được hiển thị đúng trong Excel (WrapText=true).
                    - **CLO-Mapping**: các mã CLO cách nhau bằng dấu phẩy, VD: `CLO1, CLO2`.
                    - Dữ liệu được sắp xếp tăng dần theo Session Number.
                    """
    )
    public ResponseEntity<byte[]> exportSessions(@PathVariable UUID syllabusId) {
        byte[] excelBytes = sessionService.exportSessionsToExcel(syllabusId);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        httpHeaders.setContentDispositionFormData("attachment", "Sessions_Export.xlsx");
        httpHeaders.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, httpHeaders, HttpStatus.OK);
    }

    // ------------------------------------------------------------------ //
    //                      IMPORT FROM EXCEL                             //
    // ------------------------------------------------------------------ //

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(
            summary = "Import danh sách Session từ file Excel",
            description = """
                    Upload file Excel (.xlsx) để import hàng loạt Session vào một Syllabus.
                    
                    **Cấu trúc file Excel (bắt đầu từ dòng 2, dòng 1 là header):**
                    | Cột 0 | Cột 1 | Cột 2 | Cột 3 | Cột 4 | Cột 5 |
                    |---|---|---|---|---|---|
                    | Session Number | Title | Teaching Methods | Topic | Type | CLO-Mapping |
                    
                    - **Type**: THEORY | PRACTICE | SELF_STUDY
                    - **CLO-Mapping**: Các mã CLO cách nhau bởi dấu phẩy, VD: `CLO1, CLO2`
                    
                    **Luồng xử lý:**
                    1. Đọc và parse dữ liệu Excel.
                    2. Validate CLO — kiểm tra mã CLO có hợp lệ với Subject không.
                    3. Validate Quota — kiểm tra tổng tiết lý thuyết/thực hành so với quy định.
                    4. Nếu có lỗi → trả về **HTTP 400** với danh sách lỗi chi tiết, KHÔNG lưu DB.
                    5. Nếu hợp lệ → xóa Session cũ (Replace mode) và lưu mới vào DB.
                    
                    **Response khi lỗi (HTTP 400):** `isValid = false`, `errors` chứa danh sách lỗi.
                    **Response khi thành công (HTTP 200):** `isValid = true`, `savedCount` = số session đã lưu.
                    """
    )
    public ResponseEntity<ResponseObject<SessionImportResult>> importSessions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("syllabusId") UUID syllabusId,
            @RequestParam("subjectId") UUID subjectId) {

        SessionImportResult result = sessionService.importSessionsFromExcel(file, syllabusId, subjectId);

        if (!result.isValid()) {
            // Có lỗi validate → trả về 400 Bad Request kèm chi tiết lỗi
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseObject.<SessionImportResult>builder()
                            .status(4000)
                            .message("Import failed due to validation errors. No data was saved.")
                            .data(result)
                            .build());
        }

        // Thành công → 200 OK
        return ResponseEntity.ok(
                ResponseObject.<SessionImportResult>builder()
                        .status(1000)
                        .message("Import successful. " + result.getSavedCount() + " session(s) saved.")
                        .data(result)
                        .build()
        );
    }


}

