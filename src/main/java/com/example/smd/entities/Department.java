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
@Table(name = "department")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "department_id")
    UUID departmentId;

    @Column(name = "department_code", unique = true, nullable = false, length = 20)
    String departmentCode;

    @Column(name = "department_name", nullable = false, length = 100)
    String departmentName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    List<Subject> subjects;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

