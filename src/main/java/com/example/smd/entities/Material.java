package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "material")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID materialId;

    @Column(nullable = false, length = 100, name = "material_name")
    String title;

    @Column(name = "material_type", length = 50)
    String materialType;

    @Column(name = "upload_date")
    java.time.Instant uploadedAt;

    @Column(name = "id")
    Integer id;

    @Column(name = "version")
    Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    List<Blocks> blocks;


}
