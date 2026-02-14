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
@Table(name = "assessments")
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Assessment_Category assessmentCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    Assessment_Type assessmentType;

    @Column(name = "part")
    Integer part;

    @Column(name = "weight")
    Double weight; // Decimal

    @Column(name = "completion_criteria", columnDefinition = "TEXT")
    String completionCriteria;

    @Column(name = "duration")
    Integer duration;

    @Column(name = "question_type", length = 50)
    String questionType;

    @Column(name = "knowledge_skill", length = 50)
    String knowledgeSkill;

    @Column(name = "grading_guide", columnDefinition = "TEXT")
    String gradingGuide;

    @Column(columnDefinition = "TEXT")
    String note;
}
