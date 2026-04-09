# SMD WebSocket Realtime System — Hướng Dẫn Sử Dụng

**Cập nhật: April 8, 2026 — Spring Boot 3.5.6 + STOMP/SockJS**

---

## 📋 Mục Lục

1. [Giới Thiệu](#giới-thiệu)
2. [Kiến Trúc](#kiến-trúc)
3. [Backend Setup](#backend-setup)
4. [Frontend Setup](#frontend-setup)
5. [Sử Dụng Thực Tế](#sử-dụng-thực-tế)
6. [API Reference](#api-reference)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Giới Thiệu

WebSocket được tích hợp vào SMD để hỗ trợ **realtime notifications** mà không cần polling liên tục.

### Khi nào dùng WebSocket?

- ✅ Thông báo realtime (notification mới)
- ✅ Update trạng thái (syllabus submitted, task assigned)
- ✅ Broadcast hệ thống
- ✅ Private messages tới user cụ thể

### Khi nào không dùng WebSocket?

- ❌ CRUD thông thường (dùng REST API)
- ❌ Download file lớn
- ❌ Batch operation

**Tech Stack:**

- Backend: Spring Boot 3.5.6, STOMP broker, SockJS
- Frontend: stompjs 7.0.0, sockjs-client 1.6.1
- Protocol: STOMP (Simple Text Oriented Messaging Protocol)

---

## 🏗️ Kiến Trúc

### Backend Layers

```
┌─ WebSocketConfig (config/)
│  └─ Cấu hình STOMP endpoints: /ws, /ws-native
│  └─ Cấu hình message broker prefix
│
├─ NotificationTopicRegistry (realtime/)
│  └─ Quản lý constants cho các topics
│  └─ Ví dụ: /topic/notification/account/{id}
│
├─ RealtimePayload (realtime/)
│  └─ DTO chuẩn cho tất cả messages
│  └─ Fields: code, message, timestamp, data
│
├─ RealtimePublisher (realtime/)
│  └─ Service publish để gửi messages
│  └─ Methods: publishToAccount(), broadcastToDepartment(), etc
│
├─ RealtimeInboundController (controller/)
│  └─ STOMP controller xử lý incoming messages
│  └─ @MessageMapping("/notification/...")
│
└─ NotificationService (services/)
   └─ Tích hợp realtimePublisher
   └─ Khi tạo notification, tự động publish WebSocket
```

### Topic Naming Convention

```
/topic/notification/account/{accountId}          ← Notification cho user
/topic/notification/department/{deptId}          ← Notification cho khoa
/topic/notification/syllabus/{syllabusId}        ← Notification cho đề cương
/topic/notification/task/{taskId}                ← Notification cho task
/topic/notification/review/{reviewId}            ← Notification cho review
/topic/notification/broadcast/system             ← Broadcast toàn hệ thống
/topic/event/syllabus/{syllabusId}              ← Event cho syllabus
/topic/event/task/{taskId}                       ← Event cho task
/user/{username}/queue/notifications             ← Private message
```

### Payload Format

Tất cả messages được wrap trong `RealtimePayload`:

```json
{
  "code": "NOTIFICATION_CREATED",
  "message": "Thông báo mới từ khoa",
  "timestamp": "2026-04-08T10:20:30Z",
  "data": {
    "notificationId": "uuid",
    "title": "...",
    "message": "...",
    "type": "TASK_ASSIGNED",
    "isRead": false,
    "createdAt": "2026-04-08T10:20:30Z"
  }
}
```

---

## 🔧 Backend Setup

### 1. Dependency (Đã Thêm)

File [pom.xml](../pom.xml) đã có:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 2. Configuration

File [WebSocketConfig.java](src/main/java/com/example/smd/config/WebSocketConfig.java):

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Đăng ký STOMP endpoints
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "https://your-domain.com"
                )
                .withSockJS();  // Fallback cho trình duyệt cũ
    }

    // Cấu hình message broker
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");  // Topics công khai
        registry.setApplicationDestinationPrefixes("/app");  // Inbound
        registry.setUserDestinationPrefix("/user");  // Private messages
    }
}
```

**Giải thích:**

- `/ws`: Endpoint WebSocket mà client kết nối tới
- `/topic`: Prefix cho broadcast messages (ai subscribe cũng nhận)
- `/app`: Prefix cho inbound (client gửi lên)
- `/user`: Prefix cho private messages

### 3. Topics & Constants

File [NotificationTopicRegistry.java](src/main/java/com/example/smd/realtime/NotificationTopicRegistry.java):

```java
public final class NotificationTopicRegistry {
    public static class Notification {
        public static String forAccount(String accountId) {
            return "/topic/notification/account/" + accountId;
        }

        public static String broadcastSystem() {
            return "/topic/notification/broadcast/system";
        }
    }
}
```

**Sử dụng:**

```java
// Thay vì hardcode: "/topic/notification/account/123"
String topic = NotificationTopicRegistry.Notification.forAccount("123");
```

### 4. Payload DTO

File [RealtimePayload.java](src/main/java/com/example/smd/realtime/RealtimePayload.java):

```java
@Data
@Builder
public class RealtimePayload {
    private String code;           // "NOTIFICATION", "TASK_ASSIGNED", etc
    private String message;        // Mô tả
    private Instant timestamp;     // Khi publish
    private Object data;           // Payload cụ thể

    // Factory methods
    public static RealtimePayload notification(String message, Object data) {
        return RealtimePayload.builder()
                .code("NOTIFICATION")
                .message(message)
                .data(data)
                .build();
    }
}
```

### 5. Publisher Service

File [RealtimePublisher.java](src/main/java/com/example/smd/realtime/RealtimePublisher.java):

```java
@Service
@RequiredArgsConstructor
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // Publish notification tới account
    public void publishToAccount(String accountId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forAccount(accountId);
        publish(destination, payload);
    }

    // Broadcast tới department
    public void broadcastToDepartment(String deptId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forDepartment(deptId);
        publish(destination, payload);
    }

    // Generic publish
    private void publish(String destination, RealtimePayload payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }
}
```

### 6. Controller STOMP

File [RealtimeInboundController.java](src/main/java/com/example/smd/controller/RealtimeInboundController.java):

Xử lý incoming messages từ client:

```java
@Controller
@RequiredArgsConstructor
public class RealtimeInboundController {

    private final RealtimePublisher realtimePublisher;

    // Client gửi: /app/notification/test/{accountId}
    @MessageMapping("/notification/test/{accountId}")
    public void sendTestNotification(@DestinationVariable String accountId, String msg) {
        RealtimePayload payload = RealtimePayload.notification("Test: " + msg, null);
        realtimePublisher.publishToAccount(accountId, payload);
    }

    // Client gửi: /app/event/broadcast-system
    @MessageMapping("/event/broadcast-system")
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')")
    public void broadcastToSystem(String msg) {
        RealtimePayload payload = RealtimePayload.event("BROADCAST", "System: " + msg, null);
        realtimePublisher.broadcastToSystem(payload);
    }
}
```

### 7. Integration với Notification Service

File [NotificationService.java](src/main/java/com/example/smd/services/NotificationService.java):

Khi tạo notification, tự động publish WebSocket:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RealtimePublisher realtimePublisher;

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // ... tạo notification ...
        Notification saved = notificationRepository.save(notification);

        // Publish realtime
        RealtimePayload payload = RealtimePayload.notification(
            saved.getTitle(),
            notificationMapper.toNotificationResponse(saved)
        );
        realtimePublisher.publishToAccount(
            account.getAccountId().toString(),
            payload
        );

        return response;
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        // ... đánh dấu đã đọc ...

        // Publish event
        RealtimePayload payload = RealtimePayload.event(
            "NOTIFICATION_READ",
            "Marked as read",
            response
        );
        realtimePublisher.publishToAccount(userId.toString(), payload);

        return response;
    }
}
```

---

## 🌐 Frontend Setup

### 1. Install Dependencies

```bash
npm install @stomp/stompjs sockjs-client
```

Hoặc dùng CDN:

```html
<script src="https://unpkg.com/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
```

### 2. Setup STOMP Client

**File: `src/services/webSocketService.ts`** (TypeScript/React)

```typescript
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

interface RealtimePayload {
  code: string
  message: string
  timestamp: string
  data: any
}

class WebSocketService {
  private client: Client | null = null
  private messageHandlers: Map<string, Function[]> = new Map()

  // Khởi tạo WebSocket connection
  connect(jwtToken?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        brokerURL: 'ws://localhost:8080/ws', // Hoặc wss:// cho SSL
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected')
          resolve()
        },
        onStompError: (error) => {
          console.error('STOMP error:', error)
          reject(error)
        },
      })

      this.client.activate()
    })
  }

  // Đăng ký nghe topic
  subscribe(
    topic: string,
    callback: (message: RealtimePayload) => void
  ): () => void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected')
      return () => {}
    }

    const unsubscribe = this.client.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body) as RealtimePayload
        callback(payload)
      } catch (e) {
        console.error('Error parsing message:', e)
      }
    })

    return () => unsubscribe.unsubscribe()
  }

  // Gửi message tới server (STOMP inbound)
  send(destination: string, message: any): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected')
      return
    }

    this.client.publish({
      destination,
      body: typeof message === 'string' ? message : JSON.stringify(message),
    })
  }

  // Disconnect
  disconnect(): void {
    if (this.client?.connected) {
      this.client.deactivate()
    }
  }
}

export default new WebSocketService()
```

### 3. Usage trong React

**File: `src/hooks/useNotifications.ts`**

```typescript
import { useEffect, useState } from 'react'
import webSocketService from '../services/webSocketService'

interface Notification {
  notificationId: string
  title: string
  message: string
  isRead: boolean
  createdAt: string
}

export function useNotifications(accountId: string) {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    // Kết nối WebSocket
    webSocketService.connect().then(() => {
      // Đăng ký nghe notification cho account hiện tại
      const topic = `/topic/notification/account/${accountId}`

      const unsubscribe = webSocketService.subscribe(topic, (payload) => {
        console.log('New notification:', payload)

        if (payload.code === 'NOTIFICATION') {
          const newNotif = payload.data as Notification
          setNotifications((prev) => [newNotif, ...prev])
          setUnreadCount((prev) => prev + 1)
        }

        if (payload.code === 'NOTIFICATION_READ') {
          const updatedNotif = payload.data as Notification
          setNotifications((prev) =>
            prev.map((n) =>
              n.notificationId === updatedNotif.notificationId
                ? updatedNotif
                : n
            )
          )
          setUnreadCount((prev) => prev - 1)
        }
      })

      return () => unsubscribe()
    })
  }, [accountId])

  return { notifications, unreadCount }
}
```

**File: `src/components/NotificationCenter.tsx`**

```typescript
import React from 'react';
import { useNotifications } from '../hooks/useNotifications';
import webSocketService from '../services/webSocketService';

export function NotificationCenter({ accountId }: { accountId: string }) {
  const { notifications, unreadCount } = useNotifications(accountId);

  const handleSendTestNotification = () => {
    // Gửi test message tới server
    webSocketService.send(
      `/app/notification/test/${accountId}`,
      'Test message from client'
    );
  };

  return (
    <div className="notification-center">
      <h2>Notifications ({unreadCount} unread)</h2>

      <button onClick={handleSendTestNotification}>
        Send Test Notification
      </button>

      <div className="notification-list">
        {notifications.map(notif => (
          <div key={notif.notificationId} className="notification-item">
            <h3>{notif.title}</h3>
            <p>{notif.message}</p>
            <small>{new Date(notif.createdAt).toLocaleString()}</small>
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

## 🚀 Sử Dụng Thực Tế

### Scenario 1: Tạo Notification → Tự động Broadcast

**Backend (Java):**

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final RealtimePublisher realtimePublisher;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public void assignTaskToUser(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        // Gán task
        task.setAssignedTo(userId);
        taskRepository.save(task);

        // Tạo notification qua NotificationService
        // NotificationService tự động publish WebSocket
        notificationService.createNotification(
            NotificationRequest.builder()
                .accountId(userId)
                .title("New Task Assigned")
                .message("Task: " + task.getTitle())
                .type(NotificationType.TASK_ASSIGNED)
                .taskId(taskId)
                .build()
        );

        // Hoặc publish event trực tiếp
        realtimePublisher.publishTaskEvent(
            taskId.toString(),
            "TASK_ASSIGNED",
            "Task assigned to " + user.getName(),
            Map.of("taskId", taskId, "userId", userId)
        );
    }
}
```

**Frontend (React):**

```typescript
function TaskList() {
  const { notifications } = useNotifications(accountId);

  const taskNotifications = notifications.filter(
    n => n.type === 'TASK_ASSIGNED'
  );

  return (
    <div>
      {taskNotifications.map(n => (
        <Alert key={n.notificationId} severity="info">
          {n.title}: {n.message}
        </Alert>
      ))}
    </div>
  );
}
```

### Scenario 2: Broadcast Department Event

**Backend (Java):**

```java
@Service
@RequiredArgsConstructor
public class SyllabusService {

    private final RealtimePublisher realtimePublisher;

    public void publishSyllabus(UUID syllabusId, String deptId) {
        // ... publish logic ...

        // Broadcast tới khoa
        RealtimePayload payload = RealtimePayload.event(
            "SYLLABUS_PUBLISHED",
            "New syllabus available",
            Map.of("syllabusId", syllabusId)
        );

        realtimePublisher.broadcastToDepartment(deptId, payload);
    }
}
```

**Frontend (React):**

```typescript
useEffect(() => {
  webSocketService.subscribe(
    `/topic/notification/broadcast/department/${deptId}`,
    (payload) => {
      if (payload.code === 'SYLLABUS_PUBLISHED') {
        showNotification('New Syllabus', payload.message)
      }
    }
  )
}, [deptId])
```

### Scenario 3: System-wide Announcement

**Backend (Java):**

```java
@PostMapping("/admin/announcement")
@PreAuthorize("hasRole('ADMIN')")
public ResponseObject<Void> sendSystemAnnouncement(@RequestBody String message) {
    RealtimePayload payload = RealtimePayload.event(
        "SYSTEM_ANNOUNCEMENT",
        message,
        Map.of("timestamp", Instant.now())
    );

    realtimePublisher.broadcastToSystem(payload);

    return ResponseObject.<Void>builder()
            .status(1000)
            .message("Announcement sent to all users")
            .build();
}
```

**Frontend (React):**

```typescript
useEffect(() => {
  webSocketService.subscribe(
    `/topic/notification/broadcast/system`,
    (payload) => {
      if (payload.code === 'SYSTEM_ANNOUNCEMENT') {
        showToast(payload.message, { position: 'top', type: 'info' })
      }
    }
  )
}, [])
```

---

## 📚 API Reference

### Backend Methods

#### RealtimePublisher

| Method                                              | Description                      |
| --------------------------------------------------- | -------------------------------- |
| `publishToAccount(accountId, payload)`              | Gửi tới user cụ thể              |
| `publishToDepartment(deptId, payload)`              | Gửi tới tất cả users của khoa    |
| `publishToSyllabus(syllabusId, payload)`            | Gửi tới subscribers của đề cương |
| `publishToTask(taskId, payload)`                    | Gửi tới subscribers của task     |
| `broadcastToDepartment(deptId, payload)`            | Broadcast tới khoa               |
| `broadcastToSystem(payload)`                        | Broadcast toàn hệ thống          |
| `publishSyllabusEvent(syllabusId, code, msg, data)` | Publish syllabus event           |
| `publishTaskEvent(taskId, code, msg, data)`         | Publish task event               |
| `publishReviewEvent(reviewId, code, msg, data)`     | Publish review event             |

#### RealtimePayload Factory Methods

```java
// Tạo notification payload
RealtimePayload.notification(String message, Object data)

// Tạo event payload
RealtimePayload.event(String code, String message, Object data)

// Tạo status payload
RealtimePayload.status(String status, Object data)

// Tạo error payload
RealtimePayload.error(String code, String message)
```

### Frontend Methods

#### WebSocketService

```typescript
// Kết nối WebSocket
connect(jwtToken?: string): Promise<void>

// Đăng ký nghe topic (trả về function unsubscribe)
subscribe(topic: string, callback: (payload) => void): () => void

// Gửi message tới server
send(destination: string, message: any): void

// Ngắt kết nối
disconnect(): void
```

### STOMP Endpoints

| Client Sends                               | Server Broadcasts                                   | Purpose           |
| ------------------------------------------ | --------------------------------------------------- | ----------------- |
| `/app/notification/ping/{accountId}`       | `/topic/notification/account/{accountId}`           | Ping connection   |
| `/app/notification/test/{accountId}`       | `/topic/notification/account/{accountId}`           | Test notification |
| `/app/event/broadcast-department/{deptId}` | `/topic/notification/broadcast/department/{deptId}` | Broadcast to dept |
| `/app/event/broadcast-system`              | `/topic/notification/broadcast/system`              | System broadcast  |

---

## ✅ Testing

### 1. Backend Testing

**File: Test WebSocket Connection**

```bash
# Terminal 1: Start backend
mvn spring-boot:run

# Terminal 2: Test STOMP endpoint
npx wscat -c ws://localhost:8080/ws
```

Khi kết nối:

```
Connected
> CONNECT
accept-version:1.0,1.1,1.2
```

Server trả:

```
CONNECTED
version:1.2
...
```

### 2. Frontend Testing (HTML + JavaScript)

**File: `test-websocket.html`**

```html
<!DOCTYPE html>
<html>
  <head>
    <title>WebSocket Test</title>
    <script src="https://unpkg.com/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
  </head>
  <body>
    <h1>SMD WebSocket Test</h1>

    <div>
      <input
        type="text"
        id="accountId"
        placeholder="Account ID"
        value="123e4567-e89b-12d3-a456-426614174000"
      />
      <button onclick="connect()">Connect</button>
      <button onclick="disconnect()">Disconnect</button>
    </div>

    <div>
      <input type="text" id="testMessage" placeholder="Test message" />
      <button onclick="sendTestNotification()">Send Test</button>
    </div>

    <div>
      <h2>Messages:</h2>
      <ul id="messages"></ul>
    </div>

    <script>
      let client = null
      let accountId = null

      function connect() {
        accountId = document.getElementById('accountId').value
        const socket = new SockJS('http://localhost:8080/ws')

        client = new StompJs.Client({
          webSocketFactory: () => socket,
          reconnectDelay: 5000,
          onConnect: onConnected,
          onStompError: onError,
        })

        client.activate()
      }

      function onConnected() {
        console.log('Connected')
        addMessage('✅ Connected to WebSocket')

        // Subscribe to topic
        const topic = `/topic/notification/account/${accountId}`
        client.subscribe(topic, onMessageReceived)
        addMessage(`📡 Subscribed to: ${topic}`)
      }

      function onMessageReceived(message) {
        const payload = JSON.parse(message.body)
        console.log('Received:', payload)
        addMessage(`📨 ${payload.code}: ${payload.message}`)
      }

      function sendTestNotification() {
        const message = document.getElementById('testMessage').value
        const destination = `/app/notification/test/${accountId}`

        client.publish({
          destination,
          body: message,
        })

        addMessage(`➡️  Sent to: ${destination}`)
      }

      function disconnect() {
        if (client) {
          client.deactivate()
          addMessage('❌ Disconnected')
        }
      }

      function addMessage(msg) {
        const li = document.createElement('li')
        li.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`
        document.getElementById('messages').appendChild(li)
      }

      function onError(error) {
        console.error('Error:', error)
        addMessage(`⚠️  Error: ${error}`)
      }
    </script>
  </body>
</html>
```

**Cách dùng:**

1. Mở file trong trình duyệt
2. Nhập Account ID
3. Click "Connect"
4. Nhập message và click "Send Test"
5. Xem messages được nhận

### 3. Postman Testing

1. Tạo HTTP request tới: `POST http://localhost:8080/api/notifications`
2. Body (JSON):

```json
{
  "accountId": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Test Notification",
  "message": "This is a test",
  "type": "TASK_ASSIGNED"
}
```

3. Frontend sẽ nhận message realtime

---

## 🔍 Troubleshooting

### Lỗi: STOMP Connection Timeout

**Nguyên nhân:** WebSocket server không chạy hoặc port sai

**Giải pháp:**

```bash
# Check port 8080 đang chạy không
netstat -an | findstr 8080  # Windows
lsof -i :8080              # Mac/Linux
```

### Lỗi: CORS Error

**Nguyên nhân:** Origin không được allow

**Giải pháp:** Thêm domain vào `WebSocketConfig.java`:

```java
registry.addEndpoint("/ws")
    .setAllowedOrigins(
        "https://your-actual-frontend-domain.com"
    )
```

### Lỗi: Message không được nhận

**Nguyên nhân:**

- Topic subscription sai
- Server chưa publish

**Giải pháp:**

```typescript
// Kiểm tra topic name
console.log('Subscribing to:', `/topic/notification/account/${accountId}`)

// Kiểm tra message log từ server
// application.yaml: logging.level.com.example.smd=DEBUG
```

### Lỗi: Memory Leak (Browser)

**Nguyên nhân:** Không unsubscribe khi component unmount

**Giải pháp:**

```typescript
useEffect(() => {
  const unsubscribe = webSocketService.subscribe(topic, handler)

  return () => {
    unsubscribe() // ✅ Clean up
  }
}, [topic])
```

### Notification Publish Lỗi (Backend)

**Giải pháp 1:** Check logs

```bash
# application.yaml
logging:
  level:
    com.example.smd.realtime: DEBUG
```

**Giải pháp 2:** Wrap try-catch trong NotificationService

```java
try {
    realtimePublisher.publishToAccount(accountId.toString(), payload);
} catch (Exception e) {
    log.error("Error publishing: {}", e.getMessage());
    // Notification vẫn lưu vào DB
}
```

---

## 📝 Checklist Production

- [ ] CORS domain đúng (không dùng `*`)
- [ ] WebSocket endpoint `/ws` mở qua firewall
- [ ] SSL/TLS (wss://) nếu frontend dùng https
- [ ] Heartbeat cấu hình: 4000ms
- [ ] Reconnect delay: 5000ms
- [ ] Message logging enabled
- [ ] Error handling comprehensive
- [ ] Unsubscribe khi component unmount
- [ ] Server đang chạy & lắng nghe port 8080
- [ ] Database transaction commit trước publish (nếu cần)

---

## 🔗 Resources

- [Spring WebSocket Docs](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol](http://stomp.github.io/)
- [stompjs Docs](https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html)
- [SockJS](https://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html)

---

**Cập nhật: April 8, 2026 | Dành cho SMD Team**
