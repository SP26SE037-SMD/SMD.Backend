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
public class Elective {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String electiveId;

    @Column(name = "elective_code", nullable = false, length = 20)
    String electiveCode;

    @Column(name = "elective_name", length = 100)
    String electiveName;

    @Column(columnDefinition = "TEXT")
    String description;

//    @OneToMany(mappedBy = "elective", fetch = FetchType.LAZY)
//    List<Elective_Subject> electiveSubjects;
}
