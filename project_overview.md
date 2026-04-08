# Tổng Quan Dự Án SMD — Syllabus Management and Development System

## 1. Giới Thiệu

**SMD** (Syllabus Management and Development) là hệ thống **quản lý và phát triển đề cương môn học** dành cho các cơ sở giáo dục đại học. Dự án được xây dựng bằng **Spring Boot 3.5.6 / Java 21**, kết nối cơ sở dữ liệu **PostgreSQL**, và tích hợp **Gemini AI** (Google) để hỗ trợ sinh CLO tự động và so sánh đề cương.

---

## 2. Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Framework | Spring Boot 3.5.6 |
| Ngôn ngữ | Java 21 |
| Database | PostgreSQL (pgvector extension) |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway (V1→V28) |
| Security | Spring Security + OAuth2 Resource Server (JWT) |
| API Docs | SpringDoc OpenAPI / Swagger |
| Email | Spring Mail + Gmail API (OAuth2) |
| AI | Google Gemini API (gemini-2.5-flash, gemini-embedding-001) |
| Excel | Apache POI |
| Mapping | MapStruct + Lombok |
| Container | Docker + Docker Compose |

---

## 3. Kiến Trúc Phân Lớp

```
src/main/java/com/example/smd/
├── config/          # Cấu hình: Security, JWT, Gemini, Gmail, OpenAPI, CORS
├── controller/      # REST Controllers (37 controller)
├── dto/
│   ├── request/     # DTO nhận dữ liệu từ client
│   ├── response/    # DTO trả về cho client
│   └── excel/       # DTO xuất/nhập Excel
├── entities/        # JPA Entities (46 entity)
├── enums/           # Enums (21 enum)
├── exception/       # Custom Exception + Error Codes
├── mapper/          # MapStruct mappers
├── repositories/    # JPA Repositories (42 repo)
├── services/        # Business Logic (42 service + excelService)
└── templeteEmail/   # Template email
```

---

## 4. Domain Model — Các Entity Chính

### 4.1 Quản lý Tổ chức
| Entity | Mô tả |
|---|---|
| `Department` | Khoa/Bộ môn |
| `Major` | Ngành học |
| `Account` | Tài khoản người dùng (email, password, role, department) |
| `Role` / `Permission` | Phân quyền (RoleName: HOPDC, ...) |

### 4.2 Chương trình đào tạo
| Entity | Mô tả |
|---|---|
| `Curriculum` | Chương trình đào tạo (code, tên, năm bắt đầu/kết thúc, Major) |
| `Curriculum_Group_Subject` | Nhóm môn học trong chương trình (mapping N-N) |
| `PO` | Program Outcomes — Chuẩn đầu ra chương trình |
| `PLOs` | Program Learning Outcomes |
| `PO_PLO_Mapping` | Mapping PO ↔ PLO |

### 4.3 Môn học & Đề cương
| Entity | Mô tả |
|---|---|
| `Subject` | Môn học (code, tên, tín chỉ, khoa, trạng thái, bloom level) |
| `Subject_Prerequisite` | Tiên quyết môn học |
| `Syllabus` | Đề cương môn học (thuộc Subject, trạng thái workflow) |
| `CLOs` | Course Learning Outcomes (thuộc Subject, bloom level) |
| `CLO_PLO_Mapping` | Mapping CLO ↔ PLO |

### 4.4 Nội dung Đề cương
| Entity | Mô tả |
|---|---|
| `Session` | Buổi học (số thứ tự, tiêu đề chương, thời lượng, phương pháp dạy) |
| `CLO_Session` | Mapping CLO ↔ Buổi học |
| `Blocks` | Block nội dung bên trong Session |
| `Session_Material_Block` | Mapping tài liệu ↔ Block ↔ Session |
| `Material` | Tài liệu học (link, loại, trạng thái) |
| `Source` / `Syllabus_Source` | Nguồn tham khảo cho đề cương |
| `Assessment` | Bài kiểm tra / đánh giá |
| `Assessment_Category` / `Assessment_Type` | Phân loại đánh giá |
| `CLO_Assessment` | Mapping CLO ↔ Assessment |
| `Regulation` | Quy chế liên quan đến đề cương |

### 4.5 Quy trình Review & Phê duyệt
| Entity | Mô tả |
|---|---|
| `Task` | Nhiệm vụ được giao (gắn với Sprint/Account/Syllabus/Subject) |
| `ReviewTask` | Phiếu review (comment, trạng thái chấp nhận/từ chối, reviewer) |
| `Sprint` | Sprint quản lý công việc (gắn với Account, Curriculum) |
| `Syllabus_Action_Logs` | Lịch sử thao tác trên Syllabus |
| `Syllabus_Review` | Phiếu review chính thức |
| `Syllabus_Comments` | Bình luận trên đề cương |

### 4.6 Phản hồi & Thông báo
| Entity | Mô tả |
|---|---|
| `FeedbackTemplate` / `Curriculum_Feedback_Question` | Mẫu câu hỏi phản hồi |
| `FeedbackSubmissions` / `FeedbackAnswers` | Phản hồi từ người dùng |
| `Notification` | Thông báo hệ thống |
| `System_Log` | Log hệ thống |

### 4.7 AI & Vector Search
| Entity | Mô tả |
|---|---|
| `Vector_Embeddings` | Embedding vector của Block content (pgvector, 3072 chiều) |

---

## 5. Workflow Trạng Thái Đề Cương (SyllabusStatus)

```
DRAFT → IN_PROGRESS → PENDING_REVIEW
                            ↓
                    REVISION_REQUESTED  ← (yêu cầu chỉnh sửa)
                            ↓
                     APPROVED / REJECTED
                            ↓
                        PUBLISHED
                            ↓
                         ARCHIVED
```

**SyllabusActionType** (Actions logged):
`CREATE → UPDATE → DEVELOP → SUBMIT → ASSIGN_REVIEW → START_REVIEW → REQUEST_REVISION / APPROVE / REJECT → PUBLISH → ARCHIVE`

---

## 6. Security

- **JWT** (OAuth2 Resource Server) — Custom `JwtDecoder`, `JwtAuthenticationConverter`
- **BCrypt** password encoder (strength 10)
- **CORS** cho phép: `localhost:3000`, `localhost:5173`, `localhost:3001`, `localhost:8081`, `localhost:8082`
- Endpoint công khai: `/api/auth/login`, `/api/auth/login-google`, `/api/auth/introspect`, `/api/auth/logout`, `/api/auth/me`, `/api/auth/password-reset`, Swagger UI
- Role-based access: `@PreAuthorize` (method security)
- `InvalidatedToken` để blacklist JWT đã logout

---

## 7. Tích Hợp AI (Gemini)

| Chức năng | API |
|---|---|
| Sinh CLO tự động | `gemini-2.5-flash` — nhận tên môn, chủ đề, bloom level, mô tả PLO |
| Kiểm tra/validate CLO | `gemini-flash-latest` |
| So sánh 2 đề cương | `gemini-2.5-flash` — nhận JSON 2 cấu trúc, trả `ComparisonResult` |
| Phân tích impact | `gemini-2.5-flash` — phân tích gap giữa khái niệm |
| Vector Embedding | `gemini-embedding-001` — tạo vector 3072 chiều cho nội dung Block |

---

## 8. Công Cụ & Tính Năng Khác

- **Flyway**: 28 migration scripts (V1→V28) quản lý schema từ đầu
- **Excel Import/Export**: Apache POI (`excelService/`)
- **Email**: Gmail OAuth2 API và Spring Mail; HTML templates trong `templeteEmail/`
- **Swagger UI**: `/swagger-ui.html` — mô tả đầy đủ API
- **Docker**: `Dockerfile` + `docker-compose.yml`

---

## 9. Danh Sách Controller (37 endpoints nhóm)

Các domain chính được expose qua REST API:
`Account`, `Authentication`, `AssessmentCategory/Type/Assessment`, `Block`, `CLOs`, `CloAssessmentMapping`, `CloPloMapping`, `CloSessionMapping`, `Curriculum`, `CurriculumGroupSubject`, `Department`, `Feedback`, `Group`, `Major`, `Material`, `Notification`, `PLOs`, `POs`, `PoPloMapping`, `Permission`, `Prerequisite`, `Regulation`, `ReviewTask`, `Role`, `Session`, `SessionMaterialBlock`, `Source`, `Sprint`, `Subject`, `SyllabusActionLog`, `Syllabus`, `SyllabusSource`, `SystemLog`, `Task`

---

## 10. Biến Môi Trường Cần Cấu Hình

```env
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SIGNER_KEY=...
GEMINI_API_KEY=...
CLIEND_ID=...        # Google OAuth2
CLIEND_SECRET=...
REFRESH_TOKEN=...
SENDER_EMAIL=...
```
