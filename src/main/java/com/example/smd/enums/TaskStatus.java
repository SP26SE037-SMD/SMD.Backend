package com.example.smd.enums;

public enum TaskStatus {
    DRAFT,           // Tạo bản thảo
    TO_DO,           // Chưa bắt đầu
    IN_PROGRESS,     // Đang thực hiện
    DONE,            // Hoàn thành
    CANCELLED,       // Đã hủy
    OVERDUE          // Quá hạn (do Job tự động set, dùng làm anti-spam flag)
}
