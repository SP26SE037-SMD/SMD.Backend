package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Blocks {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID blockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    Material material;

    @Column(name = "block_style", length = 50)
    String blockStyle;

    @Column(name = "idx", nullable = false)
    Integer idx; // Cột mới thay thế block_sequence

    @Column(name = "content", columnDefinition = "TEXT")
    String contentText;

    @OneToMany(mappedBy = "block", fetch = FetchType.LAZY)
    List<Session_Material_Block> sessionMaterialBlocks;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.idx == null) this.idx = 0;
    }
}
