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
public class System_Log {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    Account account;

    @Column(name = "action", nullable = false, length = 100)
    String action;

    @Column(name = "object_name", length = 100)
    String objectName; // Tên đối tượng bị tác động (ví dụ: Syllabus ABC)

    @Column(name = "created_at")
    java.time.Instant createdAt;
}
