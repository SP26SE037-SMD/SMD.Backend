package com.example.smd.controller;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.request.BulkUpdateBlockRequest;
import com.example.smd.dto.request.BlockSingleRequest;
import com.example.smd.dto.request.UpdateBlockRequest;
import com.example.smd.dto.response.BlockResponse;
import com.example.smd.dto.response.BlockSimpleResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Block", description = "Quản lý các khối nội dung của tài liệu")
@SecurityRequirement(name = "bearerAuth")
public class BlockController {
    BlockService blockService;

    @PostMapping("/material/{materialId}")
    @PreAuthorize("hasAuthority('BLOCK_CREATE')")
    @Operation(
            summary = "Tạo danh sách các khối nội dung (Bulk Create Blocks)",
            description = "### Quy trình khởi tạo nội dung tài liệu:\n" +
                    "Gửi lên một danh sách các khối nội dung. Hệ thống sẽ tự động tính toán số thứ tự (**idx**) dựa trên vị trí của phần tử trong mảng (bắt đầu từ 0).\n\n" +
                    "| Block Type | Mô tả cách hiển thị |\n" +
                    "| :--- | :--- |\n" +
                    "| **H1** | Tiêu đề chính, cỡ chữ lớn nhất. |\n" +
                    "| **H2** | Tiêu đề phụ, dùng cho các mục nhỏ. |\n" +
                    "| **PARAGRAPH** | Văn bản thông thường, hỗ trợ xuống dòng. |\n" +
                    "| **ORDERED_LIST** | Danh sách có đánh số (1, 2, 3...). |\n" +
                    "| **BULLET_LIST** | Danh sách dấu chấm đầu dòng. |\n" +
                    "| **CODE_BLOCK** | Khối mã nguồn, hiển thị font chữ Mono. |\n" +
                    "| **QUOTE** | Đoạn trích dẫn, có đường kẻ lề trái. |\n" +
                    "| **TABLE** | Dữ liệu bảng (dưới dạng Markdown hoặc JSON String). |\n" +
                    "| **DIVIDER** | Đường kẻ ngang phân cách các phần. |\n" +
                    "\n**Lưu ý:** Nếu tài liệu đã có blocks cũ, bạn nên cân nhắc logic ghi đè hoặc bổ sung tùy theo yêu cầu nghiệp vụ."
    )
    public ResponseObject<List<BlockResponse>> createBlocks(
            @PathVariable UUID materialId,
            @RequestBody List<BlockRequest> requests) {
        return ResponseObject.<List<BlockResponse>>builder()
                .status(1000)
                .data(blockService.createBlocks(materialId, requests))
                .build();
    }

    @PostMapping("/material/{materialId}/single")
    @PreAuthorize("hasAuthority('BLOCK_CREATE')")
    @Operation(
            summary = "Tạo danh sách các khối nội dung (Bulk Create Blocks)",
            description = "### Quy trình khởi tạo nội dung tài liệu:\n" +
                    "Gửi lên một danh sách các khối nội dung. Hệ thống sẽ tự động tính toán số thứ tự (**idx**) dựa trên vị trí của phần tử trong mảng (bắt đầu từ 0).\n\n" +
                    "| Block Type | Mô tả cách hiển thị |\n" +
                    "| :--- | :--- |\n" +
                    "| **H1** | Tiêu đề chính, cỡ chữ lớn nhất. |\n" +
                    "| **H2** | Tiêu đề phụ, dùng cho các mục nhỏ. |\n" +
                    "| **PARAGRAPH** | Văn bản thông thường, hỗ trợ xuống dòng. |\n" +
                    "| **ORDERED_LIST** | Danh sách có đánh số (1, 2, 3...). |\n" +
                    "| **BULLET_LIST** | Danh sách dấu chấm đầu dòng. |\n" +
                    "| **CODE_BLOCK** | Khối mã nguồn, hiển thị font chữ Mono. |\n" +
                    "| **QUOTE** | Đoạn trích dẫn, có đường kẻ lề trái. |\n" +
                    "| **TABLE** | Dữ liệu bảng (dưới dạng Markdown hoặc JSON String). |\n" +
                    "| **DIVIDER** | Đường kẻ ngang phân cách các phần. |\n" +
                    "\n**Lưu ý:** Nếu tài liệu đã có blocks cũ, bạn nên cân nhắc logic ghi đè hoặc bổ sung tùy theo yêu cầu nghiệp vụ."
    )
    public ResponseObject<BlockResponse> createSingleBlocks(
            @PathVariable UUID materialId,
            @RequestBody BlockSingleRequest requests) {
        return ResponseObject.<BlockResponse>builder()
                .status(1000)
                .data(blockService.createSingleBlock(requests, materialId))
                .build();
    }

    @GetMapping("/material/{materialId}")
    @Operation(
            summary = "Lấy danh sách blocks theo Material (Phân trang)",
            description = "Kết quả trả về được đóng gói trong PagedResponse, sắp xếp theo thứ tự idx."
    )
    public ResponseObject<PagedResponse<BlockResponse>> getAllByMaterial(
            @PathVariable UUID materialId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseObject.<PagedResponse<BlockResponse>>builder()
                .status(1000)
                .data(blockService.getAllByMaterial(materialId, page, size))
                .message("Truy vấn danh sách blocks thành công")
                .build();
    }

    @GetMapping("/material/{materialId}/by-type")
    @Operation(summary = "Lấy danh sách block theo materialId và blockType")
    public ResponseObject<List<BlockSimpleResponse>> getByMaterialAndType(
            @PathVariable UUID materialId,
            @RequestParam String blockType
    ) {
        return ResponseObject.<List<BlockSimpleResponse>>builder()
                .status(1000)
                .data(blockService.getBlocksByMaterialAndType(materialId, blockType))
                .message("Truy vấn danh sách block theo materialId và blockType thành công")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết một block")
    public ResponseObject<BlockResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<BlockResponse>builder()
                .status(1000)
                .data(blockService.getDetail(id))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật nội dung một block")
    @PreAuthorize("hasAuthority('BLOCK_UPDATE')")
    public ResponseObject<BlockResponse> update(@PathVariable UUID id, @RequestBody BlockRequest request) {
        return ResponseObject.<BlockResponse>builder()
                .status(1000)
                .data(blockService.updateBlock(id, request))
                .build();
    }

    @PutMapping("/update-list")
    @Operation(summary = "Cập nhật nội dung một block")
    @PreAuthorize("hasAuthority('BLOCK_UPDATE')")
    public ResponseObject<List<BlockResponse>> update(@RequestBody List<UpdateBlockRequest> request) {
        return ResponseObject.<List<BlockResponse>>builder()
                .status(1000)
                .data(blockService.updateBlocks(request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một block")
    @PreAuthorize("hasAuthority('BLOCK_DELETE')")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        blockService.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xóa block thành công")
                .build();
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "Xóa một block")
    @PreAuthorize("hasAuthority('BLOCK_DELETE')")
    public ResponseObject<Void> delete(@RequestBody List<UUID> blockIds) {
        blockService.deleteBlocks(blockIds);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xóa block thành công")
                .build();
    }

    @PutMapping("/material/{materialId}")
    @PreAuthorize("hasAuthority('BLOCK_UPDATE')")
    @Operation(
            summary = "Bulk Update blocks theo Material (xóa + upsert)",
            description = "### Quy trình cập nhật hàng loạt blocks của một tài liệu (material):\n" +
                    "1. **deleteBlockList**: danh sách blockId cần xóa. Hệ thống sẽ tự động kiểm tra và xóa các bản ghi liên kết trong bảng **session_material_block** trước khi xóa block.\n" +
                    "2. **blocks**: danh sách blocks cần upsert. Nếu có `blockId` → cập nhật block hiện có; không có `blockId` → tạo mới.\n\n" +
                    "**Lưu ý:** Chỉ áp dụng khi material có trạng thái `DRAFT` hoặc `REVISION_REQUESTED`."
    )
    public ResponseObject<List<BlockResponse>> bulkUpdateBlocks(
            @PathVariable UUID materialId,
            @RequestBody BulkUpdateBlockRequest request) {
        return ResponseObject.<List<BlockResponse>>builder()
                .status(1000)
                .data(blockService.bulkUpdateBlocks(materialId, request))
                .message("Cập nhật blocks thành công")
                .build();
    }
}
