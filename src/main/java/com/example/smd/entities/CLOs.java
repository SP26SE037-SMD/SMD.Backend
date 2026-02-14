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
@Table(name = "clos") // Hoặc "clo" tùy convention
public class CLOs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String cloId;

    @Column(name = "clo_code", nullable = false, length = 20)
    String cloCode;

    @Column(name = "clo_name", nullable = false, length = 100)
    String cloName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "bloom_level", length = 50)
    String bloomLevel;

    // QUAN TRỌNG: Sửa từ Syllabus thành Subject theo yêu cầu của bạn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    Subject subject;

    @OneToMany(mappedBy = "clo", fetch = FetchType.LAZY)
    List<CLO_PLO_Mapping> cloPloMappings;

//    @OneToMany(mappedBy = "clo", fetch = FetchType.LAZY)
//    List<CLO_Assessment> cloAssessments;
}
