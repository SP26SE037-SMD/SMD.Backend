package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import org.mapstruct.TargetType;
import java.util.*;

@Table(name = "feedback_answer")
public class FeedbackAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private FeedbackSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FeedbackTemplate question;

    // Lưu giá trị của option đã chọn (VD: "Trùng lặp nhẹ")
    private String selectedValue;

    // Lưu nội dung viết vào phần Note:...................
    @Column(columnDefinition = "TEXT")
    private String noteContent;

    // Dùng cho câu hỏi thuần tự luận (VD: Câu 3 của sinh viên)
    @Column(columnDefinition = "TEXT")
    private String essayAnswer;
}
