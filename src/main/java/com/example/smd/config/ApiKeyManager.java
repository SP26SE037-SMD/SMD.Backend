package com.example.smd.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyManager {

    @Value("${gemini.api-key}")
    private List<String> apiKey;

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // Lấy key đang được kích hoạt ở thời điểm hiện tại
    public String getCurrentKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Chưa cấu hình danh sách API Key!");
        }
        return apiKey.get(Math.abs(currentIndex.get() % apiKey.size()));
    }

    // Hàm xoay sang key tiếp theo (chỉ gọi hàm này khi key đang dùng bị lỗi 429)
    public synchronized void rotateKey(String failedKey) {
        // Chỉ xoay nếu key bị lỗi trùng với key hiện tại
        if (failedKey.equals(getCurrentKey())) {
            int newIndex = currentIndex.incrementAndGet();
            System.err.println("Key lỗi 429. Đã xoay sang key mới ở index: " + Math.abs(newIndex % apiKey.size()));
        }
    }

    // Lấy tổng số lượng key để giới hạn vòng lặp retry
    public int getTotalKeys() {
        return apiKey == null ? 0 : apiKey.size();
    }
}
