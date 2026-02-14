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
public class Student_Material_Tracking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String trackingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    Material material;

    @Column(name = "last_accessed")
    java.time.Instant lastAccessed;

    @Column(name = "last_page_read")
    Integer lastPageRead;

    @Column(name = "progress_percentage")
    Double progressPercentage; // Decimal(5,2)
}
