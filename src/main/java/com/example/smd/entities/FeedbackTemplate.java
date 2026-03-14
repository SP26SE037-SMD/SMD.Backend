package com.example.smd.entities;


import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "feedback_templates")
public class FeedbackTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID questionId;

    private String targetType; // STUDENT hoặc EXPERT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String optionsList; // Lưu dạng: "Rất hợp lý;Bình thường;Bất hợp lý"

    private Boolean hasNote; // Đánh dấu câu này có phần Note:... hay không

    private Integer orderIndex; // Thứ tự hiển thị câu 1, 2, 3...
}

