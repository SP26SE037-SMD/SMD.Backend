# WebSocket Integration Guide (Reusable for Other Systems)

## 1. Muc tieu tai lieu
Tai lieu nay mo ta cach ap dung WebSocket (STOMP) theo mo hinh da dung trong ScoreLens-BE de dua vao he thong khac.

Ban se co:
- Kien truc tong the
- Quy tac dat endpoint/topic
- Mau backend (Spring Boot)
- Mau client (Web/Mobile)
- Bao mat, scale, monitoring
- Checklist trien khai production

## 2. Khi nao nen dung WebSocket
Dung WebSocket khi can:
- Realtime UI (notification, event stream, status camera/device, score update)
- Giam polling REST lien tuc
- Broadcast den nhieu client dang subscribe cung mot kenh

Khong nen dung WebSocket cho:
- CRUD thong thuong
- Batch lon, khong can realtime

## 3. Kien truc de xuat
Mo hinh de tai su dung:
1. Producer event (DB/Kafka/business service) tao event.
2. Notification service publish event qua STOMP broker.
3. Client subscribe theo topic co namespace ro rang.
4. Optional: dong thoi gui FCM/APNS de danh thuc mobile background.

Luong tong quat:
- Server endpoint handshake: /ws (web), /ws-native (mobile native)
- App inbound prefix: /app
- Broker outbound prefix: /topic

## 4. Quy tac dat ten topic (rat quan trong)
Nen chuan hoa topic theo resource va context:
- /topic/notification/{resourceId}
- /topic/logging/{resourceId}
- /topic/event/{resourceId}
- /topic/match/{resourceId}

Khuyen nghi:
- Khong hardcode tung cho trong service, dung enum/constants.
- Goi payload theo mau co field code + data de de mo rong backward-compatible.

Vi du payload:
```json
{
  "code": "NOTIFICATION",
  "data": {
    "message": "Camera connected",
    "time": "2026-04-08T10:20:30Z"
  }
}
```

## 5. Backend setup (Spring Boot + STOMP)
Them dependency:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 5.1 Cau hinh WebSocket
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "https://your-frontend.com")
                .withSockJS();

        registry.addEndpoint("/ws-native")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### 5.2 Service publish thong diep
```java
@Service
@RequiredArgsConstructor
public class RealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String destination, Object message) {
        messagingTemplate.convertAndSend(destination, message);
    }
}
```

### 5.3 STOMP inbound handler (neu can client -> server)
```java
@Controller
@RequiredArgsConstructor
public class RealtimeInboundController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/noti.send/{resourceId}")
    public void sendNoti(@DestinationVariable String resourceId, String message) {
        messagingTemplate.convertAndSend("/topic/notification/" + resourceId, message);
    }
}
```

Luu y:
- Neu dung @DestinationVariable thi path trong @MessageMapping phai co placeholder tuong ung.
- Vi du dung /noti.send/{resourceId} thi client publish den /app/noti.send/{resourceId}.

## 6. Client setup (Web)
Su dung stompjs + sockjs:
```html
<script src="https://unpkg.com/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
```

```javascript
const resourceId = "TABLE_01";
const socket = new SockJS("https://your-backend.com/ws");

const client = new StompJs.Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000
});

client.onConnect = () => {
  client.subscribe(`/topic/notification/${resourceId}`, frame => {
    const body = frame.body;
    console.log("Notification:", body);
  });

  client.publish({
    destination: `/app/noti.send/${resourceId}`,
    body: "hello from client"
  });
};

client.activate();
```

## 7. Tich hop vao he thong khac theo tung buoc
1. Xac dinh event nao can realtime (notification, status, telemetry, score...).
2. Chuan hoa schema payload (code, data, timestamp, version).
3. Tao WebSocketConfig va endpoint /ws.
4. Tao constants/enum cho topics.
5. Tao publisher service (SimpMessagingTemplate wrapper).
6. Noi event business vao publisher (sau khi transaction commit neu can).
7. Them STOMP inbound handler neu can client gui len server.
8. Mo route trong security config cho /ws va /ws-native.
9. Cau hinh CORS dung domain frontend that su.
10. Viet test page hoac script subscribe/publish de smoke test.

## 8. Security va governance
Khuyen nghi production:
- Khong set allowedOrigins("*") cho web endpoint.
- Xac thuc JWT trong handshake hoac channel interceptor.
- Tach topic public/private. Voi private, can authorize theo user/session.
- Gan correlationId/requestId trong payload de trace.
- Gioi han message size va tan suat publish (rate limit) de tranh spam.

## 9. Scale va reliability
Neu tai nho-vua:
- enableSimpleBroker du dung.

Neu tai lon/nhieu instance:
- Dung message broker ngoai (RabbitMQ/ActiveMQ) cho STOMP relay.
- Hoac publish-subscribe qua Kafka + websocket gateway layer.

Khuyen nghi them:
- Heartbeat monitoring
- Retry/backoff client reconnect
- Dead-letter hoac fallback push notification cho mobile offline

## 10. Monitoring va debug
Can co cac log sau:
- Ket noi/nguon goc endpoint
- Subscribe destination
- Publish destination + message code
- Error STOMP frame

Metrics nen thu:
- So ket noi dang mo
- Messages/s theo topic
- Ty le loi publish
- Latency tu event business den client nhan duoc

## 11. Checklist go-live
- [ ] Endpoint /ws ket noi duoc tu frontend
- [ ] Topic naming da chuan hoa
- [ ] Security route da permit dung muc
- [ ] CORS da dung domain that
- [ ] Reconnect client da on
- [ ] Co test timeout/heartbeat
- [ ] Co logging + metrics
- [ ] Co fallback mobile push neu can

## 12. Mau adapter de ban tai su dung nhanh
Tao package rieng, vi du:
- config/WebSocketConfig
- realtime/TopicRegistry
- realtime/RealtimePublisher
- realtime/RealtimePayload
- realtime/InboundController

Muc tieu la khi sang he thong moi, ban chi can:
1. Doi ten topic
2. Noi event business vao RealtimePublisher
3. Doi policy security/cors

---
Neu can, ban co the tach tai lieu nay thanh 2 phien ban:
- Ban "Quick Start" (5 phut)
- Ban "Production Hardening" (bao mat + scale + SLO)
