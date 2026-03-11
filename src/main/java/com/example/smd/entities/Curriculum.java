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
@Table(name = "curriculum")
public class Curriculum {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID curriculumId;

    @Column(name = "curriculum_code", unique = true, nullable = false, length = 20)
    String curriculumCode;

    @Column(name = "curriculum_name", nullable = false, length = 100)
    String curriculumName;

    @Column(name = "start_year")
    Integer startYear;

    @Column(name = "end_year")
    Integer endYear;

    @Column(length = 20)
    String status; // Có thể dùng Enum nếu muốn

    @OneToMany(mappedBy = "curriculum", fetch = FetchType.LAZY)
    private List<Curriculum_Combo_Subject> curriculumComboSubjects;
}
