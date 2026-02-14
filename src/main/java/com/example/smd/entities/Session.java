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
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @Column(name = "session_no")
    Integer sessionNo;

    @Column(name = "topic", length = 100)
    String topic;

    @Column(name = "learning_method", length = 100)
    String learningMethod;

    @Column(name = "construction_question", length = 200)
    String constructionQuestion;
}
