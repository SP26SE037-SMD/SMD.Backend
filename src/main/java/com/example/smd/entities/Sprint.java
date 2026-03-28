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
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID sprintId;

    @Column(name = "sprint_name", nullable = false, length = 100)
    String sprintName;

    @Column(name = "start_date")
    Instant startDate;

    @Column(name = "end_date")
    Instant endDate;

    @Column(length = 20)
    String status; // Planning, Active, Completed

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    List<Task> tasks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id")
    Curriculum curriculum;

}
