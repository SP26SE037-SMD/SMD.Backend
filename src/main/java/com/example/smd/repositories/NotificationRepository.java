package com.example.smd.repositories;

import com.example.smd.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Lấy tất cả thông báo của một user (có phân trang)
    Page<Notification> findByAccountAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    // Lấy thông báo chưa đọc của user
    Page<Notification> findByAccountAccountIdAndIsReadOrderByCreatedAtDesc(UUID accountId, Boolean isRead, Pageable pageable);

    // Đếm số lượng thông báo chưa đọc
    long countByAccountAccountIdAndIsRead(UUID accountId, Boolean isRead);

    // Tìm kiếm thông báo theo tiêu đề hoặc nội dung
    @Query("SELECT n FROM Notification n WHERE n.account.accountId = :accountId " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchNotifications(@Param("accountId") UUID accountId,
                                           @Param("search") String search,
                                           Pageable pageable);
}
