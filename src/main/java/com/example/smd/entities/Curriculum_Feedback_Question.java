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
@Table(name = "curriculum_feedback_question")
public class Curriculum_Feedback_Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "question_id")
    UUID id;


    @Column(name = "question_no")
    Integer questionNo;

    @Column(name = "question_text", columnDefinition = "TEXT")
    String questionText;

    @Column(name = "question_type", length = 100)
    String questionType;

    @Column(name = "form_type", length = 100)
    String formType;

    @Column(name = "is_required")
    Boolean isRequired;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    List<FeedbackAnswers> feedbackAnswers;


    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

}
