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
public class Syllabus_Action_Logs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String actionLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by", nullable = false)
    Account actionBy;

    @Column(name = "action", nullable = false, length = 50)
    String action; // CREATE, UPDATE, APPROVE, REJECT

    @Column(name = "created_at")
    java.time.Instant createdAt;

    @Column(columnDefinition = "TEXT")
    String note;
}
