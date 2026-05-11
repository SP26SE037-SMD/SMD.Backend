package com.example.smd.services;

import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.entities.TaskV2;
import com.example.smd.enums.NotificationType;
import com.example.smd.repositories.TaskV2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * =====================================================================
 * TaskReminderJob - Cron Job quét và nhắc nhở Task định kỳ
 * =====================================================================
 *
 * Chạy mỗi ngày lúc 8:00 sáng (cron = "0 0 8 * * *").
 * Xử lý 3 mốc thời gian theo đúng nghiệp vụ:
 *
 *  [MỐC 1] dueDate = today + 3  -> Gửi In-App Notification (nhắc sớm)
 *  [MỐC 2] dueDate = today      -> Gửi In-App Notification (đúng hạn chót)
 *  [MỐC 3] dueDate < today      -> Gửi Notification + Email (đã quá hạn)
 *                                  + cơ chế chống spam: chỉ gửi 1 lần duy nhất
 *
 * NGUYÊN TẮC THIẾT KẾ:
 *  - DB-level filtering: Mọi điều kiện lọc ngày đều viết trong @Query,
 *    KHÔNG bao giờ dùng findAll() rồi filter trong Java.
 *  - No for-loop email: Gửi email theo batch (CompletableFuture song song),
 *    không dùng for-loop tuần tự cho email.
 *  - Anti-spam: Task quá hạn chỉ gửi email đúng 1 lần, sau đó đánh dấu
 *    isOverdueNotified=true và cập nhật status=OVERDUE bằng JPQL BULK UPDATE.
 * =====================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskReminderJob {

    private final TaskV2Repository taskV2Repository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /** Formatter hiển thị ngày trong notification & email */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Entry point của Cron Job.
     * Cron expression: "0 0 8 * * *" = 8:00:00 sáng mỗi ngày.
     *
     * Luồng xử lý:
     * 1. Lấy ngày hôm nay (LocalDate.now()).
     * 2. Gọi 3 query DB để lấy đúng danh sách task thuộc 3 mốc.
     * 3. Xử lý song song: notification (in-app) cho mốc 1&2, notification+email cho mốc 3.
     * 4. Bulk-update DB để đánh dấu task quá hạn đã gửi email (chống spam).
     *
     * @Transactional đảm bảo @Modifying (markOverdueNotified) được commit đúng cách.
     */
    @Scheduled(cron = "0 15 11 * * *")
    @Transactional
    public void runTaskReminderJob() {
        LocalDate today = LocalDate.now();
        log.info("===== [TaskReminderJob] STARTED - Date: {} =====", today);

        try {
            // ----------------------------------------------------------------
            // BƯỚC 1: Lấy 3 danh sách từ DB (DB tự filter, không load thừa RAM)
            // ----------------------------------------------------------------
            // MỐC 1: Truyền today+3 vào query (JPQL không hỗ trợ date arithmetic)
            List<TaskV2> threeDayTasks  = taskV2Repository.findTasksDueInThreeDays(today.plusDays(3));
            List<TaskV2> dueTodayTasks  = taskV2Repository.findTasksDueToday(today);
            List<TaskV2> overdueTaskList = taskV2Repository.findOverdueTasksNotYetNotified(today);

            log.info("[TaskReminderJob] 3-day tasks: {} | Due today: {} | Overdue (unsent): {}",
                    threeDayTasks.size(), dueTodayTasks.size(), overdueTaskList.size());

            // ----------------------------------------------------------------
            // BƯỚC 2: Xử lý MỐC 1 - Còn 3 ngày (In-App Notification only)
            // ----------------------------------------------------------------
            sendBulkNotifications(
                    threeDayTasks,
                    "⏰ Task sắp đến hạn trong 3 ngày",
                    task -> String.format(
                            "Task \"%s\" sẽ đến hạn vào ngày %s. Hãy hoàn thành đúng tiến độ!",
                            task.getTaskName(),
                            task.getDueDate().format(DATE_FORMATTER)
                    ),
                    NotificationType.REMINDER
            );

            // ----------------------------------------------------------------
            // BƯỚC 3: Xử lý MỐC 2 - Đúng ngày tới hạn (In-App Notification only)
            // ----------------------------------------------------------------
            sendBulkNotifications(
                    dueTodayTasks,
                    "🔔 Task đến hạn hôm nay!",
                    task -> String.format(
                            "Task \"%s\" đến hạn hôm nay (%s). Vui lòng hoàn thành và cập nhật trạng thái!",
                            task.getTaskName(),
                            task.getDueDate().format(DATE_FORMATTER)
                    ),
                    NotificationType.DEADLINE
            );

            // ----------------------------------------------------------------
            // BƯỚC 4: Xử lý MỐC 3 - Đã quá hạn (Notification + Email batch)
            // ----------------------------------------------------------------
            if (!overdueTaskList.isEmpty()) {
                processOverdueTasks(overdueTaskList, today);
            }

        } catch (Exception e) {
            log.error("[TaskReminderJob] CRITICAL ERROR during job execution: {}", e.getMessage(), e);
            // Không re-throw để không crash ứng dụng, job sẽ tự chạy lại vào ngày hôm sau
        }

        log.info("===== [TaskReminderJob] FINISHED =====");
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Gửi In-App Notification hàng loạt cho một danh sách task.
     * Dùng stream + lambda thay vì for-loop.
     * Mỗi notification gửi riêng lẻ, lỗi một cái không ảnh hưởng cái khác.
     *
     * @param tasks          Danh sách task cần thông báo
     * @param title          Tiêu đề thông báo
     * @param messageBuilder Lambda tạo nội dung thông báo từ task
     * @param type           Loại notification (REMINDER / DEADLINE)
     */
    private void sendBulkNotifications(List<TaskV2> tasks,
                                       String title,
                                       java.util.function.Function<TaskV2, String> messageBuilder,
                                       NotificationType type) {
        tasks.forEach(task -> {
            try {
                // Lấy accountId của người được giao task
                UUID assigneeId = task.getAccount() != null
                        ? task.getAccount().getAccountId()
                        : null;

                if (assigneeId == null) {
                    log.warn("[TaskReminderJob] Task {} has no assignee, skipping notification.", task.getTaskId());
                    return; // forEach -> next item
                }

                NotificationRequest request = NotificationRequest.builder()
                        .title(title)
                        .message(messageBuilder.apply(task))
                        .type(type)
                        .accountId(assigneeId)
                        .build();

                notificationService.createNotification(request);
                log.debug("[TaskReminderJob] Notification sent for task: {}", task.getTaskId());
            } catch (Exception e) {
                // Lỗi một task không chặn toàn bộ batch
                log.error("[TaskReminderJob] Failed to send notification for task {}: {}",
                        task.getTaskId(), e.getMessage());
            }
        });
    }

    /**
     * Xử lý toàn bộ luồng cho MỐC 3 (quá hạn):
     *  1. Gửi In-App Notification cho tất cả task quá hạn.
     *  2. Build danh sách TaskOverdueEmail và gửi email batch song song.
     *  3. Bulk-update DB: đánh dấu isOverdueNotified=true + status=OVERDUE
     *     bằng một câu JPQL UPDATE duy nhất (tránh N+1 save).
     *
     * @param overdueTaskList Danh sách task quá hạn chưa gửi email
     * @param today           Ngày hiện tại (dùng tính số ngày trễ)
     */
    private void processOverdueTasks(List<TaskV2> overdueTaskList, LocalDate today) {

        // --- 4a. Gửi In-App Notification cho toàn bộ task quá hạn ---
        sendBulkNotifications(
                overdueTaskList,
                "🚨 Task đã quá hạn!",
                task -> String.format(
                        "Task \"%s\" đã quá hạn %d ngày (hạn chót: %s). Vui lòng xử lý ngay!",
                        task.getTaskName(),
                        ChronoUnit.DAYS.between(task.getDueDate(), today),
                        task.getDueDate().format(DATE_FORMATTER)
                ),
                NotificationType.DEADLINE
        );

        // --- 4b. Chuẩn bị danh sách email batch (không dùng for-loop) ---
        //         Chỉ gửi email cho task có email của assignee
        List<EmailService.TaskOverdueEmail> emailPayloads = overdueTaskList.stream()
                .filter(task -> task.getAccount() != null
                        && task.getAccount().getEmail() != null
                        && !task.getAccount().getEmail().isBlank())
                .map(task -> {
                    long overdueDays = ChronoUnit.DAYS.between(task.getDueDate(), today);
                    String displayName = task.getAccount().getFullName() != null
                            ? task.getAccount().getFullName()
                            : task.getAccount().getEmail();
                    return new EmailService.TaskOverdueEmail(
                            task.getAccount().getEmail(),
                            displayName,
                            task.getTaskName(),
                            task.getDueDate().format(DATE_FORMATTER),
                            overdueDays
                    );
                })
                .toList();

        // --- 4c. Gửi email batch song song (CompletableFuture, không block Job) ---
        emailService.sendTaskOverdueEmailsBatch(emailPayloads);

        // --- 4d. BULK UPDATE: Đánh dấu đã gửi email + set status=OVERDUE ---
        //         1 câu JPQL UPDATE duy nhất thay vì save() N lần -> tối ưu DB
        List<UUID> processedIds = overdueTaskList.stream()
                .map(TaskV2::getTaskId)
                .toList();

        taskV2Repository.markOverdueNotified(processedIds);
        log.info("[TaskReminderJob] Marked {} task(s) as OVERDUE (isOverdueNotified=true).",
                processedIds.size());
    }
}
