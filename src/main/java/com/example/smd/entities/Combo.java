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
@Table(name = "combo")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String comboId;

    @Column(name = "combo_code", nullable = false, length = 20)
    String comboCode;

    @Column(name = "combo_name", length = 100)
    String comboName;

    @Column(length = 20)
    String type; // Elective / Mandatory

    @OneToMany(mappedBy = "combo", fetch = FetchType.LAZY)
    private List<Curriculum_Combo_Subject> curriculumComboSubjects;
}
