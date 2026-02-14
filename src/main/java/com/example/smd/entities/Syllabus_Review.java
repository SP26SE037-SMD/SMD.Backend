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
public class Syllabus_Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    Account reviewer;

    @Column(name = "review_status", length = 20)
    String reviewStatus; // Pending, Approved, Rejected

    @Column(name = "created_at")
    java.time.Instant createdAt;

    @OneToMany(mappedBy = "syllabusReview", fetch = FetchType.LAZY)
    List<Syllabus_Comments> comments;
}
