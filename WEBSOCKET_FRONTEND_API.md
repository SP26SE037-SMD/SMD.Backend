# SMD WebSocket Frontend API Contract

## 1. Mục tiêu tài liệu
Tài liệu này liệt kê các API mà frontend cần dùng khi tích hợp realtime với WebSocket/STOMP trong SMD.

Phạm vi:
- Endpoint kết nối WebSocket
- Destination client gửi lên server
- Topic frontend subscribe để nhận dữ liệu
- Cấu trúc payload realtime
- REST API liên quan để đồng bộ notification list

Không bao gồm:
- Hướng dẫn theo framework cụ thể
- Hướng dẫn cấu hình chi tiết frontend build tool

---

## 2. Kết nối WebSocket

### 2.1 Handshake endpoints
- `/ws`
  - Dùng cho web client, có SockJS fallback.
- `/ws-native`
  - Dùng cho native client (mobile/desktop).

### 2.2 Prefix quy ước
- Application prefix (client publish): `/app`
- Broker prefix (client subscribe): `/topic`
- User destination prefix (private message): `/user`

---

## 3. Client Publish API (STOMP Inbound)

Frontend gửi message lên các destination sau (prepend `/app`):

1. Ping connection
- Destination: `/app/notification/ping/{accountId}`
- Body: text tùy ý
- Server phản hồi về: `/topic/notification/account/{accountId}`

2. Test notification
- Destination: `/app/notification/test/{accountId}`
- Body: text message
- Server publish về: `/topic/notification/account/{accountId}`

3. Broadcast theo department
- Destination: `/app/event/broadcast-department/{departmentId}`
- Body: text message
- Ghi chú: endpoint có kiểm tra quyền ở backend.
- Server publish về: `/topic/notification/broadcast/department/{departmentId}`

4. Broadcast toàn hệ thống
- Destination: `/app/event/broadcast-system`
- Body: text message
- Ghi chú: endpoint có kiểm tra quyền ở backend.
- Server publish về: `/topic/notification/broadcast/system`

5. Ack subscribe (optional)
- Destination: `/app/subscription/ack/{topicType}/{resourceId}`
- Body: text tùy ý
- Mục đích: logging phía server, không trả payload nghiệp vụ.

---

## 4. Client Subscribe API (STOMP Outbound Topics)

### 4.1 Notification topics
1. Theo user
- Topic: `/topic/notification/account/{accountId}`
- Khi dùng: inbox notification cá nhân, unread badge, toast cá nhân.

2. Theo department
- Topic: `/topic/notification/department/{departmentId}`
- Khi dùng: nhóm người dùng cùng khoa nhận thông báo chung.

3. Theo syllabus
- Topic: `/topic/notification/syllabus/{syllabusId}`
- Khi dùng: màn hình chi tiết đề cương cần realtime update.

4. Theo task
- Topic: `/topic/notification/task/{taskId}`
- Khi dùng: màn hình task detail/kanban cần theo dõi task cụ thể.

5. Theo review
- Topic: `/topic/notification/review/{reviewId}`
- Khi dùng: màn hình review detail cần update tức thời.

6. Broadcast department
- Topic: `/topic/notification/broadcast/department/{departmentId}`
- Khi dùng: announcement cho toàn khoa.

7. Broadcast system
- Topic: `/topic/notification/broadcast/system`
- Khi dùng: system-wide announcement.

### 4.2 Event topics
1. Syllabus event
- Topic: `/topic/event/syllabus/{syllabusId}`

2. Subject event
- Topic: `/topic/event/subject/{subjectId}`

3. Sprint event
- Topic: `/topic/event/sprint/{sprintId}`

4. Task event
- Topic: `/topic/event/task/{taskId}`

5. Review event
- Topic: `/topic/event/review/{reviewId}`

6. Curriculum event
- Topic: `/topic/event/curriculum/{curriculumId}`

### 4.3 Status topics
1. Resource status
- Topic: `/topic/status/{resourceType}/{resourceId}`

2. System health
- Topic: `/topic/status/system/health`

### 4.4 Private user destination
- Topic private queue: `/user/{username}/queue/notifications`
- Ghi chú:
  - Tùy backend mapping user principal, frontend thường subscribe qua `/user/queue/notifications` theo convention STOMP user destination.
  - Chỉ dùng khi backend publish theo convertAndSendToUser.

---

## 5. Realtime Payload Contract

Tất cả payload realtime dùng format chung:

```json
{
  "code": "NOTIFICATION",
  "message": "string",
  "timestamp": "2026-04-08T10:20:30Z",
  "data": {},
  "meta": {}
}
```

### 5.1 Field definitions
- `code`: mã loại message.
  - Ví dụ: `NOTIFICATION`, `NOTIFICATION_READ`, `ALL_NOTIFICATIONS_READ`, `BROADCAST_SYSTEM`.
- `message`: mô tả ngắn.
- `timestamp`: thời điểm server phát message (UTC Instant).
- `data`: payload nghiệp vụ.
- `meta`: thông tin bổ sung (optional).

### 5.2 Message code cần frontend xử lý ngay
- `NOTIFICATION`
- `NOTIFICATION_READ`
- `ALL_NOTIFICATIONS_READ`
- `BROADCAST_DEPARTMENT`
- `BROADCAST_SYSTEM`

Lưu ý: `code` có thể mở rộng thêm theo nghiệp vụ event.

---

## 6. REST API liên quan notification (dùng kèm WebSocket)

Frontend nên kết hợp REST + WebSocket:
- REST để lấy snapshot ban đầu.
- WebSocket để nhận thay đổi realtime.

Base path: `/api/notifications`

1. Tạo notification
- `POST /api/notifications`

2. Lấy danh sách của user hiện tại
- `GET /api/notifications/my-notifications?page={page}&size={size}&isRead={bool?}`

3. Lấy chi tiết notification
- `GET /api/notifications/{id}`

4. Đánh dấu đã đọc
- `PUT /api/notifications/{id}/mark-as-read`

5. Đánh dấu tất cả đã đọc
- `POST /api/notifications/mark-all-as-read`

6. Đếm unread
- `GET /api/notifications/unread-count`

7. Tìm kiếm notification
- `GET /api/notifications/search?search={keyword}&page={page}&size={size}`

8. Lấy notification theo account
- `GET /api/notifications/account-notifications?accountId={id}&page={page}&size={size}&isRead={bool?}`

---

## 7. Luồng tích hợp chuẩn cho frontend

1. Login xong, lấy context user: `accountId`, `departmentId`, role.
2. Kết nối WebSocket handshake endpoint.
3. Subscribe tối thiểu:
- `/topic/notification/account/{accountId}`
- `/topic/notification/broadcast/system`
4. Nếu có màn hình scope cụ thể thì subscribe thêm topic theo task/review/syllabus/department.
5. Gọi REST lấy snapshot:
- danh sách notification
- unread count
6. Khi nhận message realtime:
- cập nhật store/UI ngay theo `code`.
7. Khi user mark-as-read:
- gọi REST mark-as-read.
- nhận lại event realtime để đồng bộ đa tab/device.

---

## 8. Danh sách topicType gợi ý cho frontend (dùng với ack endpoint)

Giá trị đề xuất cho `{topicType}` khi gọi `/app/subscription/ack/{topicType}/{resourceId}`:
- `account`
- `department`
- `syllabus`
- `task`
- `review`
- `broadcast_department`
- `broadcast_system`
- `event_subject`
- `event_syllabus`
- `event_sprint`
- `event_task`
- `event_review`
- `event_curriculum`
- `status_resource`
- `status_system_health`

Đây là convention để log/telemetry dễ đọc.

---

## 9. Boundary rõ ràng để tránh nhầm

- Frontend không subscribe vào `/app/...`.
- Frontend không publish vào `/topic/...`.
- WebSocket/STOMP destinations không xuất hiện như REST endpoint trong Swagger.
- Swagger chỉ phục vụ HTTP API; topic contract nằm trong tài liệu này.

---

## 10. Versioning

- Document version: `1.0`
- Last updated: `2026-04-08`
- Source of truth:
  - `src/main/java/com/example/smd/config/WebSocketConfig.java`
  - `src/main/java/com/example/smd/controller/RealtimeInboundController.java`
  - `src/main/java/com/example/smd/realtime/NotificationTopicRegistry.java`
  - `src/main/java/com/example/smd/realtime/RealtimePayload.java`
  - `src/main/java/com/example/smd/controller/NotificationController.java`
