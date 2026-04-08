package com.example.smd.realtime;

/**
 * Quản lý các STOMP topic constants cho hệ thống realtime
 * Chứa các topic được sử dụng để publish/subscribe WebSocket messages
 *
 * Quy tắc naming:
 * - /topic/notification/{resourceId}: Public notifications
 * - /topic/event/{resourceId}: Domain events
 * - /topic/status/{resourceId}: Real-time status updates
 * - /user/{username}/queue/{service}: Private user messages
 */
public final class NotificationTopicRegistry {

    private NotificationTopicRegistry() {
        // Utility class, không được khởi tạo
    }

    /**
     * Base topic paths
     */
    public static final String TOPIC_PREFIX = "/topic";
    public static final String NOTIFICATION_PREFIX = TOPIC_PREFIX + "/notification";
    public static final String EVENT_PREFIX = TOPIC_PREFIX + "/event";
    public static final String STATUS_PREFIX = TOPIC_PREFIX + "/status";

    /**
     * Notification topics
     */
    public static class Notification {
        private Notification() {}

        // Topic nhận thông báo theo account
        public static String forAccount(String accountId) {
            return NOTIFICATION_PREFIX + "/account/" + accountId;
        }

        // Topic nhận thông báo theo department
        public static String forDepartment(String departmentId) {
            return NOTIFICATION_PREFIX + "/department/" + departmentId;
        }

        // Topic nhận thông báo theo syllabus
        public static String forSyllabus(String syllabusId) {
            return NOTIFICATION_PREFIX + "/syllabus/" + syllabusId;
        }

        // Topic nhận thông báo theo task
        public static String forTask(String taskId) {
            return NOTIFICATION_PREFIX + "/task/" + taskId;
        }

        // Topic nhận thông báo theo review
        public static String forReview(String reviewId) {
            return NOTIFICATION_PREFIX + "/review/" + reviewId;
        }

        // Broadcast tới tất cả users của một khoa
        public static String broadcastDepartment(String departmentId) {
            return NOTIFICATION_PREFIX + "/broadcast/department/" + departmentId;
        }

        // Broadcast tới toàn bộ hệ thống
        public static String broadcastSystem() {
            return NOTIFICATION_PREFIX + "/broadcast/system";
        }
    }

    /**
     * Event topics
     */
    public static class Event {
        private Event() {}

        // Subject event (create, update, status change, etc.)
        public static String subjectEvent(String subjectId) {
            return EVENT_PREFIX + "/subject/" + subjectId;
        }

        // Syllabus event (tạo, update, submit, approve, etc.)
        public static String syllabusEvent(String syllabusId) {
            return EVENT_PREFIX + "/syllabus/" + syllabusId;
        }
 
        // Sprint event (create, update, status change, etc.)
        public static String sprintEvent(String sprintId) {
            return EVENT_PREFIX + "/sprint/" + sprintId;
        }

        // Task event (assign, complete, reject, etc.)
        public static String taskEvent(String taskId) {
            return EVENT_PREFIX + "/task/" + taskId;
        }

        // Review event
        public static String reviewEvent(String reviewId) {
            return EVENT_PREFIX + "/review/" + reviewId;
        }

        // Curriculum event
        public static String curriculumEvent(String curriculumId) {
            return EVENT_PREFIX + "/curriculum/" + curriculumId;
        }
    }

    /**
     * Status topics
     */
    public static class Status {
        private Status() {}

        // Status cập nhật cho resource (document, report, etc.)
        public static String forResource(String resourceType, String resourceId) {
            return STATUS_PREFIX + "/" + resourceType + "/" + resourceId;
        }

        // System health status
        public static String systemHealth() {
            return STATUS_PREFIX + "/system/health";
        }
    }
}
