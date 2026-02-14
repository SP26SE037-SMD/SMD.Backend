package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

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
    String blockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    Material material;

    @Column(name = "block_style", length = 50)
    String blockStyle;

    @Column(name = "content_text", columnDefinition = "TEXT")
    String contentText;
}
