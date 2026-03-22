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
@Table(name = "group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID groupId;

    @Column(name = "group_code", nullable = false, length = 20)
    String groupCode;

    @Column(name = "group_name", length = 100)
    String groupName;

    @Column(name = "description")
    String description;

    @Column(name = "group_type",length = 20)
    String type; // Elective / Mandatory

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Curriculum_Group_Subject> curriculumGroupSubjects;
}
