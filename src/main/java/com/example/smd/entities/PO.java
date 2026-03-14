package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "po")
public class PO {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID poId;

    @Column(name = "po_code", nullable = false, length = 20)
    String poCode;

    @Column(name = "po_name", nullable = false) // Đảm bảo nullable là true
    String poName; // Theo hình là Varchar(20) hơi ngắn, có thể nên tăng lên

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "status")
    String status;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    Major major;

}
