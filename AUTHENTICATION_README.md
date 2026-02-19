# 🔐 SMD Authentication System

Hệ thống Authentication, Authorization với JWT cho SMD Project

## 📋 Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc](#kiến-trúc)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [API Documentation](#api-documentation)
- [Sử dụng](#sử-dụng)
- [Cấu trúc Database](#cấu-trúc-database)
- [Security Configuration](#security-configuration)

---

## 🎯 Tổng quan

Hệ thống Authentication này được xây dựng với:

- **JWT (JSON Web Token)** cho authentication
- **Spring Security** cho authorization
- **Role-Based Access Control (RBAC)** với Permission system
- **BCrypt** để mã hóa password
- **Flyway** để quản lý database migration
- **OpenAPI/Swagger** để tài liệu hóa API

### Các tính năng chính:

✅ Login với username/password
✅ JWT token generation và validation
✅ Role-based và Permission-based authorization
✅ Password reset với token
✅ Account management (CRUD)
✅ Role & Permission management
✅ Global exception handling

---

## 🏗️ Kiến trúc

### 1. **Entities (Database Layer)**

```
Account (Tài khoản người dùng)
  ├── accountId (UUID)
  ├── username (unique)
  ├── email (unique)
  ├── passwordHash
  ├── fullName
  ├── isActive
  ├── role (ManyToOne -> Role)
  ├── createdAt
  └── lastLogin

Role (Vai trò)
  ├── roleId (UUID)
  ├── roleName (unique)
  ├── description
  ├── permissions (ManyToMany -> Permission)
  └── createdAt

Permission (Quyền hạn)
  ├── permissionId (UUID)
  ├── permissionName (unique)
  ├── description
  └── createdAt
```

### 2. **Security Flow**

```
Client Request
    ↓
SecurityConfig (Filter Chain)
    ↓
CustomJwtDecoder (Validate Token)
    ↓
AuthenticationService.introspect()
    ↓
Extract JWT Claims (accountId, scope/role)
    ↓
JwtAuthenticationConverter (ROLE_PREFIX)
    ↓
@PreAuthorize("hasRole('ADMIN')") - Authorization Check
    ↓
Controller → Service → Repository
    ↓
Response
```

### 3. **Các thành phần chính**

#### **Config Package:**

- `SecurityConfig.java` - Cấu hình Spring Security
- `CustomJwtDecoder.java` - Validate và decode JWT token
- `JwtAuthenticationEntryPoint.java` - Xử lý 401 Unauthenticated
- `CustomAccessDeniedHandler.java` - Xử lý 403 Forbidden
- `OpenApiConfig.java` - Cấu hình Swagger với JWT

#### **Service Layer:**

- `AuthenticationService` - Login, JWT generation, introspect
- `AccountService` - CRUD accounts
- `RoleService` - CRUD roles
- `PermissionService` - CRUD permissions

#### **Controller Layer:**

- `AuthenticationController` - `/api/v1/auth/*`
- `AccountController` - `/api/v1/accounts/*`
- `RoleController` - `/api/v1/roles/*`
- `PermissionController` - `/api/v1/permissions/*`

---

## 🚀 Cài đặt và chạy

### 1. **Yêu cầu hệ thống**

- Java 21+
- PostgreSQL 14+
- Maven 3.8+

### 2. **Cấu hình Database**

Tạo database:

```sql
CREATE DATABASE smd;
CREATE USER smd_data WITH PASSWORD '12345';
GRANT ALL PRIVILEGES ON DATABASE smd TO smd_data;
```

### 3. **Environment Variables**

Tạo file `.env` hoặc set trong environment:

```bash
JWT_SIGNER_KEY=YourSecretKeyForJWTTokenGenerationMinimum256BitsLongPlease
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USERNAME=smd_data
DB_PASSWORD=12345
```

### 4. **Chạy Migration**

```bash
# Run Flyway migration
mvn flyway:migrate

# Or run from application
mvn spring-boot:run
```

Migration V3 sẽ tự động:

- Tạo bảng `permission` và `role_permission`
- Thêm `role_id` vào bảng `account`
- Insert default roles: ADMIN, LECTURER, REVIEWER, STUDENT
- Insert default permissions
- Tạo admin account mặc định

### 5. **Khởi động ứng dụng**

```bash
mvn clean install
mvn spring-boot:run
```

Application sẽ chạy tại: `http://localhost:8080`

---

## 📚 API Documentation

### Swagger UI

Truy cập: `http://localhost:8080/swagger-ui.html`

### API Endpoints

#### 🔓 **Authentication APIs** (Public)

**1. Login**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "status": 1000,
  "message": "Login successfully",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true,
    "account": {
      "accountId": "...",
      "username": "admin",
      "email": "admin@smd.edu.vn",
      "role": {...}
    }
  }
}
```

**2. Introspect Token**

```http
POST /api/v1/auth/introspect?token=<JWT_TOKEN>

Response:
{
  "status": 1000,
  "message": "Token is valid",
  "data": true
}
```

**3. Reset Password**

```http
POST /api/v1/auth/password-reset
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**4. Get Current Account Info**

```http
GET /api/v1/auth/me?token=<JWT_TOKEN>
```

#### 🔐 **Account APIs** (Requires Authentication)

**Get All Accounts** (ADMIN only)

```http
GET /api/v1/accounts?page=0&size=10&sort=createdAt,desc
Authorization: Bearer <JWT_TOKEN>
```

**Get Account by ID** (ADMIN or Owner)

```http
GET /api/v1/accounts/{accountId}
Authorization: Bearer <JWT_TOKEN>
```

**Create Account** (ADMIN only)

```http
POST /api/v1/accounts
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "roleId": "<role-uuid>",
  "isActive": true
}
```

**Update Account** (ADMIN or Owner)

```http
PUT /api/v1/accounts/{accountId}
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "email": "newemail@example.com",
  "fullName": "John Doe Updated"
}
```

**Delete Account** (ADMIN only)

```http
DELETE /api/v1/accounts/{accountId}
Authorization: Bearer <JWT_TOKEN>
```

#### 🎭 **Role APIs** (ADMIN only)

```http
GET /api/v1/roles
GET /api/v1/roles/{roleId}
GET /api/v1/roles/name/{roleName}
POST /api/v1/roles
PUT /api/v1/roles/{roleId}
DELETE /api/v1/roles/{roleId}
```

#### 🔑 **Permission APIs** (ADMIN only)

```http
GET /api/v1/permissions
GET /api/v1/permissions/{permissionId}
GET /api/v1/permissions/name/{permissionName}
POST /api/v1/permissions
PUT /api/v1/permissions/{permissionId}
DELETE /api/v1/permissions/{permissionId}
```

---

## 🗄️ Cấu trúc Database

### Tables

```sql
-- Account table
account (
  account_id UUID PRIMARY KEY,
  username VARCHAR(50) UNIQUE,
  email VARCHAR(100) UNIQUE,
  password_hash VARCHAR(255),
  full_name VARCHAR(100),
  role_id UUID FK -> role(role_id),
  is_active BOOLEAN,
  created_at TIMESTAMP,
  last_login TIMESTAMP
)

-- Role table
role (
  role_id UUID PRIMARY KEY,
  role_name VARCHAR(50) UNIQUE,
  description TEXT,
  created_at TIMESTAMP
)

-- Permission table
permission (
  permission_id UUID PRIMARY KEY,
  permission_name VARCHAR(50) UNIQUE,
  description TEXT,
  created_at TIMESTAMP
)

-- Role-Permission junction table
role_permission (
  role_id UUID FK -> role(role_id),
  permission_id UUID FK -> permission(permission_id),
  PRIMARY KEY (role_id, permission_id)
)
```

### Default Roles

| Role     | Description                              |
| -------- | ---------------------------------------- |
| ADMIN    | System administrator với toàn quyền      |
| LECTURER | Giảng viên - Tạo và quản lý syllabus     |
| REVIEWER | Người duyệt - Review và approve syllabus |
| STUDENT  | Sinh viên - Chỉ xem syllabus             |

### Default Permissions

- `ACCOUNT_*`: CREATE, READ, UPDATE, DELETE
- `ROLE_*`: CREATE, READ, UPDATE, DELETE
- `PERMISSION_*`: CREATE, READ, UPDATE, DELETE
- `SYLLABUS_*`: CREATE, READ, UPDATE, DELETE, APPROVE, REVIEW

---

## 🔒 Security Configuration

### JWT Configuration

```yaml
jwt:
  signer-key: ${JWT_SIGNER_KEY:default-secret-key}
  valid-duration: 3600 # 1 hour
  refreshable-duration: 604800 # 7 days
```

### CORS Configuration

Cho phép origins:

- `http://localhost:3000`
- `http://localhost:5173`

### Endpoints Security

- **Public**: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Authenticated**: Tất cả endpoints khác
- **ADMIN only**: `/api/v1/accounts/**`, `/api/v1/roles/**`, `/api/v1/permissions/**`

### Password Encoding

Sử dụng BCrypt với strength = 10

---

## 📝 Sử dụng

### 1. Login và lấy token

```java
// Request
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "admin123"
}

// Response
{
  "status": 1000,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImFjY291bnRJZCI6IjEyMy...",
    "authenticated": true
  }
}
```

### 2. Sử dụng token trong requests

```bash
curl -X GET http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### 3. Swagger UI với JWT

1. Truy cập `http://localhost:8080/swagger-ui.html`
2. Click nút **"Authorize"** ở góc phải
3. Nhập token: `Bearer <your-jwt-token>`
4. Click **"Authorize"**
5. Giờ có thể test tất cả APIs

---

## 🧪 Testing

### Default Admin Account

```
Username: admin
Password: admin123
Email: admin@smd.edu.vn
Role: ADMIN
```

### Test Flow

1. **Login** với admin account
2. **Get token** từ response
3. **Create** role mới và permissions
4. **Create** account với role vừa tạo
5. **Test** authorization với account mới

---

## 🔧 Troubleshooting

### 1. "Invalid token" error

- Kiểm tra token còn hạn không (1 giờ)
- Verify JWT_SIGNER_KEY đúng

### 2. "Access Denied" error

- Kiểm tra role của account
- Verify permissions được assign cho role

### 3. Migration failed

- Check xem V1, V2 migration đã chạy chưa
- Kiểm tra database connection
- Run: `mvn flyway:repair` nếu cần

### 4. Cannot login

- Verify password encoding đúng (BCrypt)
- Check account.is_active = true
- Check role_id đã được set

---

## 📞 Support

Nếu gặp vấn đề, vui lòng:

1. Check logs: `target/logs/application.log`
2. Verify database schema
3. Test với Swagger UI
4. Contact team

---

**Developed by SMD Team - 2026**
