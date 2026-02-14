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
@Table(name = "plos")
public class PLOs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String ploId;

    @Column(name = "plo_code", nullable = false, length = 20)
    String ploCode;

    @Column(name = "plo_name", nullable = false, length = 20) // Theo hình là Varchar(20) hơi ngắn, có thể nên tăng lên
    String ploName;

    @Column(columnDefinition = "TEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    Major major;

//    @OneToMany(mappedBy = "plo", fetch = FetchType.LAZY)
//    List<CLO_PLO_Mapping> cloPloMappings;
}
