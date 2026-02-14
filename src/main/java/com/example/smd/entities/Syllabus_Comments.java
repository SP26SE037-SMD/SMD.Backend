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
public class Syllabus_Comments {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    Syllabus_Review syllabusReview;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    String content;

    @Column(name = "is_resolved")
    Boolean isResolved;

    @Column(name = "created_at")
    java.time.Instant createdAt;

}
