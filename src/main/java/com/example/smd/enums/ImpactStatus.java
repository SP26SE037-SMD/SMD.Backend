package com.example.smd.enums;

public enum ImpactStatus {
    /**
     * Không tìm thấy liên quan.
     * Môn học này không dùng đến kiến thức bị mất, cũng không dạy lại nó.
     */
    NO_IMPACT,

    /**
     * Lỗ hổng lan truyền (Nguy hiểm).
     * Môn học này YÊU CẦU sinh viên phải biết kiến thức đó để học tiếp,
     * nhưng trong giáo trình lại không hề dạy lại (Gap xuất hiện).
     */
    REQUIRED,

    /**
     * Lỗ hổng đã được vá (An toàn).
     * Môn học này có nhắc đến kiến thức bị mất, nhưng AI nhận diện được
     * là giáo trình đã TỰ DẠY LẠI kiến thức đó. Chuỗi lan truyền dừng lại ở đây.
     */
    RESOLVED
}
