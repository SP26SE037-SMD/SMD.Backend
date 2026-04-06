package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkUpdateBlockRequest {

    /** Danh sách blockId cần xóa (có thể null hoặc rỗng) */
    List<UUID> deleteBlockList;

    /** Danh sách blocks cần upsert (update nếu có blockId, insert mới nếu blockId null) */
    List<BlockUpdateItem> blocks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BlockUpdateItem {
        /** null → tạo mới; có giá trị → cập nhật block hiện có */
        UUID blockId;
        Integer idx;
        String blockStyle;
        String blockType;
        String contentText;
    }
}
