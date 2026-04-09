package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "assessment_type")
public class Assessment_Type {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID typeId;

    @Column(name = "type_name", nullable = false, length = 50)
    String typeName;
}
