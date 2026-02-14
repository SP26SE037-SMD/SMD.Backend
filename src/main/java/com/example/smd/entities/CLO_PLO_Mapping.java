package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "clo_plo_mapping")
public class CLO_PLO_Mapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id; // Synthetic ID cho bảng trung gian

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plo_id", nullable = false)
    PLOs plo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clo_id", nullable = false)
    CLOs clo;
}
