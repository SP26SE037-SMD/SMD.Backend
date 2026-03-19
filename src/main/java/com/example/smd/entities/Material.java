package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String materialId;

    @Column(nullable = false, length = 100)
    String title;

    @Column(length = 100)
    String author;

    @Column(length = 100)
    String publisher;

    @Column(name = "published_date")
    java.time.Instant publishedDate;

    @Column(name = "material_type", length = 50)
    String materialType;

    @Column(length = 100)
    String url; // hoặc "edition" tùy context

    @Column(name = "uploaded_at")
    java.time.Instant uploadedAt;

    @Column(name = "status")
    String status;

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    List<Blocks> blocks;
}
