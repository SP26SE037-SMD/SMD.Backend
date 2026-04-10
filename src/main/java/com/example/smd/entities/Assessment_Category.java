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
@Table(name = "assessment_category")
public class Assessment_Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID categoryId;

    @Column(name = "category_name", nullable = false, length = 50)
    String categoryName;

    @Column(columnDefinition = "TEXT")
    String description;
}
