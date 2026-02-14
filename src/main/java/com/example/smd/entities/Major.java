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
@Table(name = "major")
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String majorId;

    @Column(name = "major_code", unique = true, nullable = false, length = 20)
    String majorCode;

    @Column(name = "major_name", nullable = false, length = 100)
    String majorName;

    @Column(columnDefinition = "TEXT")
    String description;

    @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
    List<Curriculum> curriculums;

    @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
    List<PLOs> plos;
}
