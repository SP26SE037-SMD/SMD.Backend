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
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String departmentId;

    @Column(name = "department_name", nullable = false, length = 100)
    String departmentName;

    @Column(name = "department_code", length = 20)
    String departmentCode;
}

