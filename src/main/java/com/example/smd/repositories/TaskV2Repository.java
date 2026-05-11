package com.example.smd.repositories;

import com.example.smd.entities.TaskV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, UUID>, JpaSpecificationExecutor<TaskV2> {

    /**
     * Trả về tập tất cả targetId đã tồn tại dưới dạng task trong sprint này,
     * dùng để tránh tạo task trùng lặp khi chạy createBatch.
     */
    @Query("SELECT t.targetId FROM TaskV2 t WHERE t.sprint.sprintId = :sprintId AND t.targetId IS NOT NULL")
    Set<UUID> findAllTargetIdsInSprint(@Param("sprintId") UUID sprintId);

    // =========================================================================
    // REMINDER JOB QUERIES
    // Tất cả query dưới đây đều để DB tự filter theo ngày, không load thừa vào RAM.
    // =========================================================================

    /**
     * [MỐC 1] - Còn đúng 3 ngày tới hạn.
     * Điều kiện:
     *   - dueDate = targetDate (= today + 3, tính sẵn bên ngoài)
     *   - status KHÔNG phải DONE | CANCELLED | OVERDUE
     *
     * LƯU Ý: JPQL không hỗ trợ date arithmetic (LocalDate + 3).
     * Phải tính today.plusDays(3) trong Java rồi truyền vào đây.
     *
     * @param targetDate ngày cần kiểm tra (= today.plusDays(3))
     */
    @Query("""
            SELECT t FROM TaskV2 t
            JOIN FETCH t.account a
            WHERE t.dueDate = :targetDate
              AND t.status NOT IN ('DONE', 'CANCELLED', 'OVERDUE')
            """)
    List<TaskV2> findTasksDueInThreeDays(@Param("targetDate") LocalDate targetDate);

    /**
     * [MỐC 2] - Đúng ngày tới hạn (0 ngày).
     * Điều kiện:
     *   - dueDate = today
     *   - status KHÔNG phải DONE | CANCELLED | OVERDUE
     *
     * @param today ngày hiện tại
     */
    @Query("""
            SELECT t FROM TaskV2 t
            JOIN FETCH t.account a
            WHERE t.dueDate = :today
              AND t.status NOT IN ('DONE', 'CANCELLED', 'OVERDUE')
            """)
    List<TaskV2> findTasksDueToday(@Param("today") LocalDate today);

    /**
     * [MỐC 3] - Đã quá hạn VÀ CHƯA được xử lý (chống spam).
     * Điều kiện:
     *   - dueDate < today
     *   - status KHÔNG phải DONE | CANCELLED | OVERDUE
     *   -> 'OVERDUE' là trạng thái Job tự set sau lần gửi đầu tiên.
     *      Các lần quét sau nếu thấy OVERDUE -> bỏ qua, không gửi nữa.
     *
     * @param today ngày hiện tại
     */
    @Query("""
            SELECT t FROM TaskV2 t
            JOIN FETCH t.account a
            WHERE t.dueDate < :today
              AND t.status NOT IN ('DONE', 'CANCELLED', 'OVERDUE')
            """)
    List<TaskV2> findOverdueTasksNotYetNotified(@Param("today") LocalDate today);

    /**
     * [BULK UPDATE] - Chuyển status sang OVERDUE cho toàn bộ task quá hạn.
     * Một câu JPQL UPDATE duy nhất thay vì save() từng entity -> tối ưu DB.
     * Status = 'OVERDUE' vừa ghi nhận trạng thái, vừa làm cơ chế chống spam:
     * Query mốc 3 sẽ lọc 'OVERDUE' ra -> không bao giờ gửi lại.
     *
     * @param ids danh sách UUID của các task cần cập nhật
     */
    @Modifying
    @Query("""
            UPDATE TaskV2 t
            SET t.status = 'OVERDUE'
            WHERE t.taskId IN :ids
            """)
    void markOverdueNotified(@Param("ids") List<UUID> ids);
}
