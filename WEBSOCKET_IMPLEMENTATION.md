# WebSocket Implementation — SMD System

**Status:** ✅ Completed
**Date:** April 8, 2026
**Version:** 1.0

---

## 📌 Tóm Tắt Thay Đổi

Hệ thống SMD đã được tích hợp **WebSocket STOMP realtime notifications**. Tất cả notifications được tự động push realtime tới clients mà không cần polling.

---

## 📂 Các File Mới Được Thêm

### Backend (Java)

#### 1. **Config**

- **[WebSocketConfig.java](src/main/java/com/example/smd/config/WebSocketConfig.java)**
  - Cấu hình STOMP endpoints: `/ws`, `/ws-native`
  - Cấu hình message broker prefix
  - Cấu hình CORS

#### 2. **Realtime Package**

Thư mục mới: `src/main/java/com/example/smd/realtime/`

- **[NotificationTopicRegistry.java](src/main/java/com/example/smd/realtime/NotificationTopicRegistry.java)**
  - Quản lý topic constants
  - Methods: `Notification.forAccount()`, `broadcastSystem()`, etc
  - Tránh hardcode topic names

- **[RealtimePayload.java](src/main/java/com/example/smd/realtime/RealtimePayload.java)**
  - DTO chuẩn cho tất cả WebSocket messages
  - Fields: `code`, `message`, `timestamp`, `data`, `meta`
  - Factory methods: `notification()`, `event()`, `status()`, `error()`

- **[RealtimePublisher.java](src/main/java/com/example/smd/realtime/RealtimePublisher.java)**
  - Service publish messages
  - Methods: `publishToAccount()`, `broadcastToDepartment()`, `publishSyllabusEvent()`, etc
  - Wrapper trên `SimpMessagingTemplate`

#### 3. **Controller**

- **[RealtimeInboundController.java](src/main/java/com/example/smd/controller/RealtimeInboundController.java)**
  - Xử lý incoming STOMP messages
  - @MessageMapping endpoints: `/app/notification/...`, `/app/event/...`
  - Test endpoints: ping, test notification, broadcast

#### 4. **Services** (Modified)

- **[NotificationService.java](src/main/java/com/example/smd/services/NotificationService.java)** — Modified
  - Thêm `RealtimePublisher` dependency
  - `createNotification()` — tự động publish WebSocket
  - `markAsRead()` — publish event
  - `markAllAsRead()` — publish event

#### 5. **Dependencies** (Modified)

- **[pom.xml](pom.xml)** — Modified
  - Thêm `spring-boot-starter-websocket` dependency

### Frontend (HTML/JavaScript)

- **[websocket-test.html](src/main/resources/static/websocket-test.html)**
  - Dashboard test interaktif
  - Test connection, notifications, events, broadcasts
  - Statistics dashboard
  - Message log viewer
  - Accessible: `http://localhost:8080/websocket-test.html`

### Documentation

- **[WEBSOCKET_SMD_GUIDE.md](WEBSOCKET_SMD_GUIDE.md)** — **Đọc này trước!**
  - Hướng dẫn toàn diện (Vietnamese)
  - Architecture overview
  - Backend setup
  - Frontend setup (React hooks, vanilla JS)
  - Real-world scenarios
  - API Reference
  - Testing guide
  - Troubleshooting

---

## 🚀 Quick Start

### 1. Backend

```bash
# Build project (Maven)
mvn clean install

# Run
mvn spring-boot:run

# Hoặc qua IDE
```

Server sẽ chạy trên: `http://localhost:8080`
WebSocket endpoint: `ws://localhost:8080/ws`

### 2. Test Dashboard

1. Bật browser, truy cập: `http://localhost:8080/websocket-test.html`
2. Nhập Account ID (hoặc dùng UUID mặc định)
3. Click "Connect"
4. Gửi test notifications/events
5. Xem messages được nhận realtime ✨

### 3. Tạo Notification (API)

```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "accountId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "New Task",
    "message": "You have a new task assigned",
    "type": "TASK_ASSIGNED"
  }'
```

**Kết quả:**

- Notification được lưu vào DB ✅
- WebSocket message tự động push tới client ✅

---

## 📚 Tài Liệu

### Đọc Theo Thứ Tự

1. **[WEBSOCKET_SMD_GUIDE.md](WEBSOCKET_SMD_GUIDE.md)** ← Start here!
   - Giải thích chi tiết từng component
   - Code examples
   - Frontend integration guide

2. **Code Files**
   - Backend: `src/main/java/com/example/smd/config/WebSocketConfig.java`
   - Realtime: `src/main/java/com/example/smd/realtime/`
   - Controller: `src/main/java/com/example/smd/controller/RealtimeInboundController.java`

3. **Frontend Test**
   - `src/main/resources/static/websocket-test.html`

---

## 🔧 Architecture Overview

```
User Action (e.g., Assign Task)
    ↓
TaskService.assignTask()
    ↓
NotificationService.createNotification()
    ↓
1. Save to DB ✅
2. Call RealtimePublisher.publishToAccount()
    ↓
3. STOMP Broker sends to /topic/notification/account/{id}
    ↓
4. All subscribed clients receive message ✅
```

---

## 🎯 Use Cases

### Scenario 1: Task Assignment

**Backend (Java):**

```java
taskService.assignTaskToUser(taskId, userId);
// → NotificationService.createNotification() tự động
// → WebSocket message push realtime
```

**Frontend (React):**

```typescript
const { notifications } = useNotifications(userId)
// Subscribe to /topic/notification/account/{userId}
// Automatically update UI when new notification arrives
```

### Scenario 2: Broadcast Department Event

**Backend (Java):**

```java
realtimePublisher.broadcastToDepartment(deptId, payload);
```

**Frontend (React):**

```typescript
webSocketService.subscribe(
  `/topic/notification/broadcast/department/${deptId}`,
  (payload) => handleBroadcast(payload)
)
```

---

## ⚙️ Configuration

### Backend (application.yaml)

Không cần cấu hình thêm, mặc định đã setup. Nếu muốn log debug:

```yaml
logging:
  level:
    com.example.smd.realtime: DEBUG
    org.springframework.web.socket: DEBUG
```

### Frontend

```typescript
// websocket.service.ts
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws', // Đổi domain production
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
})
```

---

## 🔐 Security

### Authentication

- WebSocket sử dụng JWT từ HTTP Authorization header
- Mỗi subscriber được validate qua SecurityContext
- CORS: Chỉ allow domains được whitelist

### Authorization

- Topic `/topic/notification/account/{id}` — user chỉ subscribe được topic của chính mình
- `/app/event/broadcast-system` — chỉ ADMIN có thể gửi (qua @PreAuthorize)

**Cấu hình CORS (tùy domain):**

```java
// WebSocketConfig.java
registry.addEndpoint("/ws")
    .setAllowedOrigins(
        "http://localhost:3000",
        "https://your-production-domain.com"
    )
```

---

## 🧪 Testing

### Unit Test Backend

```bash
# Mock RealtimePublisher
@MockBean
private RealtimePublisher realtimePublisher;

@Test
public void testNotificationBroadcast() {
    notificationService.createNotification(request);
    verify(realtimePublisher).publishToAccount(
        eq(accountId.toString()),
        any(RealtimePayload.class)
    );
}
```

### Integration Test

```bash
# Use test HML file
Open: src/main/resources/static/websocket-test.html
1. Connect
2. Send test notification
3. Verify message received
```

### E2E Test (Frontend)

```typescript
test('receives notification when sent', async () => {
  const { getByText } = render(<NotificationCenter />);

  // Simulate notification
  webSocketService.subscribe('/topic/notification/account/123', callback);

  // Send via API
  await api.createNotification({ ... });

  // Verify UI updated
  expect(getByText('New Task')).toBeInTheDocument();
});
```

---

## 📊 Monitoring & Metrics

### Logs to Track

Enable debug logging:

```bash
logging.level.com.example.smd.realtime=DEBUG
logging.level.org.springframework.web.socket=DEBUG
```

### Key Events to Log

```
[DEBUG] Published to /topic/notification/account/xxx: NOTIFICATION_CREATED
[DEBUG] Subscription acknowledged: type=notification, resourceId=xxx
[ERROR] Error publishing to /topic/notification/account/xxx: Connection timeout
```

### Useful Metrics

- WebSocket connection count
- Messages published per second
- Error rate
- Latency (publish to receive)

---

## ⚠️ Troubleshooting

### Connection Issues

```text
❌ "WebSocket is closed before the connection is established"
→ Check: Server running? Port 8080 open? Firewall?
```

```text
❌ "CORS error"
→ Check: setAllowedOrigins() includes your frontend domain
```

### Message Not Received

```text
❌ "Subscribe but no message received"
→ Check:
   1. Topic name correct? Log it
   2. Publisher actually called? Add breakpoint
   3. Client subscribed before publish?
```

**Debug code:**

```typescript
const topic = `/topic/notification/account/${accountId}`
console.log('Subscribing to:', topic) // ← Verify topic name

webSocketService.subscribe(topic, (payload) => {
  console.log('Received:', payload) // ← Verify message
})
```

---

## 🔄 Integration Checklist

- ✅ WebSocketConfig added
- ✅ RealtimePublisher service created
- ✅ NotificationService integrated
- ✅ Test dashboard created
- ✅ Messages format standardized
- ✅ CORS configured
- ✅ Security integrated
- ✅ Documentation complete

**Next Steps:**

- [ ] Update frontend application (React/Vue/Angular)
- [ ] Add more event types as needed
- [ ] Setup production domain CORS
- [ ] Add metrics/monitoring
- [ ] Performance testing with load

---

## 📞 Support

**For questions/issues, check:**

1. [WEBSOCKET_SMD_GUIDE.md](WEBSOCKET_SMD_GUIDE.md) — Full documentation
2. Server logs: `application.yaml` with `logging.level.com.example.smd=DEBUG`
3. Browser console: WebSocket connection status
4. Test page: `websocket-test.html` — Interactive debugging

---

## 📝 File Structure Summary

```
smd/
├── src/main/
│   ├── java/com/example/smd/
│   │   ├── config/
│   │   │   └── WebSocketConfig.java ← NEW
│   │   ├── controller/
│   │   │   └── RealtimeInboundController.java ← NEW
│   │   ├── realtime/ ← NEW PACKAGE
│   │   │   ├── NotificationTopicRegistry.java
│   │   │   ├── RealtimePayload.java
│   │   │   └── RealtimePublisher.java
│   │   ├── services/
│   │   │   └── NotificationService.java ← MODIFIED
│   │   └── ...
│   └── resources/
│       ├── static/
│       │   └── websocket-test.html ← NEW
│       └── application.yaml
├── pom.xml ← MODIFIED (WebSocket dependency added)
├── WEBSOCKET_SMD_GUIDE.md ← NEW (Read this!)
└── WEBSOCKET_IMPLEMENTATION.md ← This file
```

---

**Version:** 1.0 | **Last Updated:** April 8, 2026 | **Author:** SMD Dev Team
