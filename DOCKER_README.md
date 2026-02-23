# Hướng Dẫn Docker cho SMD

## 📋 Các File Docker

1. **Dockerfile** - File cấu hình để build Docker image
2. **.dockerignore** - Loại trừ các file không cần thiết khi build
3. **docker-compose.yml** - Orchestration cho nhiều services

## 🚀 Cách Sử Dụng

### Option 1: Build và Run với Docker (chỉ app)

```bash
# Build Docker image
docker build -t smd-app:latest .

# Run container
docker run -d \
  --name smd-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/smd_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SIGNER_KEY=your-secret-key-minimum-32-characters-long \
  smd-app:latest

# Xem logs
docker logs -f smd-app

# Stop container
docker stop smd-app

# Remove container
docker rm smd-app
```

### Option 2: Sử dụng Docker Compose (app + database)

```bash
# Start tất cả services (app + postgres)
docker-compose up -d

# Xem logs
docker-compose logs -f

# Xem logs của service cụ thể
docker-compose logs -f app
docker-compose logs -f postgres

# Stop tất cả services
docker-compose down

# Stop và xóa volumes (xóa luôn database)
docker-compose down -v

# Rebuild image khi có thay đổi code
docker-compose up -d --build
```

## 🔧 Cấu Trúc Dockerfile

### Stage 1: Build Stage

- Sử dụng **maven:3.9.6-eclipse-temurin-21**
- Download dependencies trước (tận dụng cache)
- Build JAR file với Maven

### Stage 2: Runtime Stage

- Sử dụng **eclipse-temurin:21-jdk-jammy** (nhẹ hơn)
- Copy JAR file từ build stage
- Cấu hình JVM options tối ưu
- Health check endpoint

## 📝 Các Biến Môi Trường Quan Trọng

### Database

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/smd_db
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
```

### JWT Configuration

```yaml
JWT_SIGNER_KEY: your-secret-key-minimum-32-characters-long-for-hs512
JWT_VALID_DURATION: 3600 # 1 giờ
JWT_REFRESHABLE_DURATION: 86400 # 24 giờ
```

### JPA

```yaml
SPRING_JPA_HIBERNATE_DDL_AUTO: update # hoặc validate trong production
SPRING_JPA_SHOW_SQL: 'false' # true để debug
```

## 🔍 Kiểm Tra Trạng Thái

```bash
# Kiểm tra containers đang chạy
docker ps

# Kiểm tra health của app
curl http://localhost:8080/actuator/health

# Access Swagger UI
http://localhost:8080/swagger-ui.html

# Vào trong container để debug
docker exec -it smd-app bash

# Kiểm tra logs real-time
docker-compose logs -f app
```

## 🗄️ Quản Lý Database

```bash
# Connect vào PostgreSQL container
docker exec -it smd-postgres psql -U postgres -d smd_db

# Backup database
docker exec smd-postgres pg_dump -U postgres smd_db > backup.sql

# Restore database
docker exec -i smd-postgres psql -U postgres smd_db < backup.sql
```

## 🎯 Best Practices

1. **Không commit sensitive data** vào Dockerfile
2. **Sử dụng .dockerignore** để giảm kích thước image
3. **Multi-stage build** để giảm kích thước image cuối
4. **Health checks** để đảm bảo container healthy
5. **Sử dụng volumes** cho data persistence
6. **Thay đổi JWT_SIGNER_KEY** trong production

## 🐛 Troubleshooting

### Lỗi: Cannot connect to database

```bash
# Kiểm tra postgres container đã chạy chưa
docker-compose ps

# Restart postgres
docker-compose restart postgres

# Kiểm tra logs
docker-compose logs postgres
```

### Lỗi: Port already in use

```bash
# Tìm process đang dùng port 8080
netstat -ano | findstr :8080

# Thay đổi port trong docker-compose.yml
ports:
  - "8081:8080"  # map port 8081 bên ngoài
```

### Rebuild khi có thay đổi code

```bash
# Rebuild image và restart container
docker-compose up -d --build

# Hoặc force recreate
docker-compose up -d --force-recreate
```

## 📊 Giám Sát Resource

```bash
# Xem resource usage
docker stats

# Xem disk usage
docker system df

# Clean up unused resources
docker system prune -a
```

## 🚢 Deploy lên Production

### 1. Build optimized image

```bash
docker build -t smd-app:v1.0.0 .
```

### 2. Tag image cho registry

```bash
docker tag smd-app:v1.0.0 your-registry.com/smd-app:v1.0.0
```

### 3. Push lên registry

```bash
docker push your-registry.com/smd-app:v1.0.0
```

### 4. Deploy trên server

```bash
docker pull your-registry.com/smd-app:v1.0.0
docker run -d \
  --name smd-app \
  -p 8080:8080 \
  --env-file .env.production \
  your-registry.com/smd-app:v1.0.0
```

## 📚 Tài Liệu Tham Khảo

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/topicals/spring-boot-docker/)
