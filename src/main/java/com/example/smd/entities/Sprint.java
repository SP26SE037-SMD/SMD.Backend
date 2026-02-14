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
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String sprintId;

    @Column(name = "sprint_name", nullable = false, length = 100)
    String sprintName;

    @Column(name = "start_date")
    java.time.Instant startDate;

    @Column(name = "end_date")
    java.time.Instant endDate;

    @Column(length = 20)
    String status; // Planning, Active, Completed

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    List<Task> tasks;

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    List<Sprint_Member> sprintMembers;
}
