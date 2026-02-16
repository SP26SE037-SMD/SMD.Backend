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
@Table(name = "lecturer_profile")
public class Lecturer_Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lecturer_id")
    String lecturerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    Department department;

    @Column(name = "title", length = 50) // 'GS', 'PGS', 'TS', 'ThS'
    String title;

    @Column(name = "specialization", columnDefinition = "TEXT")
    String specialization;

    @Column(columnDefinition = "TEXT")
    String bio;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "lecturer", fetch = FetchType.LAZY)
    List<Sprint_Member> sprintMembers;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
