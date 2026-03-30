package com.example.smd.dto.response.session;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionMaterialBlockDetailResponse {
    UUID session;
    Integer sessionNumber;
    String sessionTitle;
    String teachingMethods;
    Integer duration;
    List<MaterialItem> material;
    List<BlockItem> block;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MaterialItem {
        UUID materialId;
        String materialName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BlockItem {
        UUID blockId;
        String content;
        Integer idx;
    }
}
