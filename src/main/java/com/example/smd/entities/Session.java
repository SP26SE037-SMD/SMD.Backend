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
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @Column(name = "session_number", nullable = false)
    Integer sessionNumber;

    @Column(name = "session_title", nullable = false, length = 200)
    String sessionTitle;

    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    String learningObjectives;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "teaching_methods", columnDefinition = "TEXT")
    String teachingMethods;

    @Column(name = "duration") // in minutes
    Integer duration;

    @Column(name = "created_at")
    Instant createdAt;

    @Column(name = "status")
    String status;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    List<CLO_Session> cloSessions;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
