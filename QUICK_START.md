# 🚀 Quick Start - Code First với Flyway

## Các File Đã Tạo

```
📁 d:\smd\
├── 📄 FLYWAY_CODE_FIRST_GUIDE.md      # Hướng dẫn chi tiết đầy đủ
├── 📄 generate-migration.ps1           # Script tự động generate migration
├── 📄 src\main\resources\
│   ├── 📄 application.yaml             # Đã cấu hình PostgreSQL + Flyway
│   └── 📁 db\migration\
│       └── 📄 V1__Initial_Schema.sql   # Baseline migration đầu tiên
```

---

## ⚡ Bắt Đầu Ngay (5 Phút)

### Bước 1: Tạo Database PostgreSQL

```powershell
# Mở psql hoặc pgAdmin, chạy:
CREATE DATABASE smd_db;
```

### Bước 2: Cấu Hình Kết Nối

Mở file `src/main/resources/application.yaml`, kiểm tra và sửa thông tin kết nối:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smd_db
    username: postgres          # ← Sửa username của bạn
    password: postgres          # ← Sửa password của bạn
```

### Bước 3: Run Migration

```powershell
# Xem trạng thái hiện tại
mvn flyway:info

# Apply migration (tạo tất cả bảng)
mvn flyway:migrate

# Kiểm tra lại
mvn flyway:info
```

### Bước 4: Chạy Application

```powershell
mvn spring-boot:run
```

✅ **Done!** Database đã được tạo với đầy đủ 40+ bảng theo thiết kế OBE.

---

## 📊 Kiểm Tra Kết Quả

### Option 1: Sử dụng psql

```sql
-- Kết nối vào database
psql -U postgres -d smd_db

-- Xem tất cả bảng
\dt

-- Xem chi tiết một bảng
\d subjects

-- Xem lịch sử migrations
SELECT * FROM flyway_schema_history;
```

### Option 2: Sử dụng pgAdmin

1. Mở pgAdmin
2. Connect đến PostgreSQL server
3. Vào database `smd_db`
4. Xem Schemas → public → Tables

---

## 🔄 Quy Trình Làm Việc Hàng Ngày

### Khi Thêm/Sửa Entity

**Ví dụ: Thêm field mới vào Subject**

1. **Sửa Entity:**
```java
@Entity
public class Subject {
    // ... existing fields
    
    @Column(name = "is_featured")
    Boolean isFeatured;  // NEW!
}
```

2. **Tạo Migration:**
```sql
-- V2__Add_Featured_To_Subject.sql
ALTER TABLE subjects ADD COLUMN is_featured BOOLEAN DEFAULT FALSE;
```

3. **Apply Migration:**
```powershell
mvn flyway:migrate
```

4. **Chạy App:**
```powershell
mvn spring-boot:run
```

---

## 🛠️ Script Tự Động (Advanced)

### Generate Migration Tự Động

```powershell
# Chạy script PowerShell
.\generate-migration.ps1 -MigrationName "Add_New_Fields" -AutoIncrement

# Script sẽ:
# 1. Backup application.yaml
# 2. Configure Hibernate để export DDL
# 3. Generate schema.sql
# 4. Tạo migration file
# 5. Restore application.yaml
```

---

## 📝 Naming Convention (Quan Trọng!)

### Format Migration File:

```
V{số}__Mô_Tả.sql
```

### Ví dụ:

- ✅ `V1__Initial_Schema.sql`
- ✅ `V2__Add_Featured_To_Subject.sql`
- ✅ `V3__Create_Index_On_Subject_Code.sql`
- ❌ `V1_initial.sql` (thiếu __)
- ❌ `migration.sql` (thiếu version)

---

## 🔍 Troubleshooting

### Lỗi: "Table already exists"

**Giải pháp:**
```powershell
# Clean và migrate lại (CHỈ trong dev)
mvn flyway:clean
mvn flyway:migrate
```

### Lỗi: "Checksum mismatch"

**Nguyên nhân:** Sửa file migration đã apply

**Giải pháp:**
```powershell
mvn flyway:repair
```

### Lỗi: Connection refused

**Kiểm tra:**
1. PostgreSQL có đang chạy không?
2. Port 5432 có bị block không?
3. Username/password đúng chưa?

```powershell
# Kiểm tra PostgreSQL service
Get-Service postgresql*
```

---

## 📚 Tài Liệu Đầy Đủ

Xem file [FLYWAY_CODE_FIRST_GUIDE.md](FLYWAY_CODE_FIRST_GUIDE.md) để biết:
- Chi tiết về quy trình code-first
- Best practices
- Advanced features
- CI/CD integration
- Rollback strategies

---

## 🎯 Next Steps

1. ✅ Database đã được tạo với V1 migration
2. 📝 Bắt đầu uncomment các relationships trong entities
3. 🔄 Khi cần thay đổi schema → Tạo migration mới (V2, V3...)
4. 🧪 Viết integration tests với Flyway Test
5. 🚀 Deploy lên production với `spring.profiles.active=prod`

---

## ⚠️ Lưu Ý Quan Trọng

### ❌ KHÔNG BAO GIỜ:

1. Sửa file migration đã apply
2. Xóa file migration đã apply
3. Dùng `ddl-auto: update` trong production
4. Run `flyway:clean` trong production

### ✅ LUÔN LUÔN:

1. Backup database trước khi migrate (production)
2. Test migration trong dev trước
3. Commit migration cùng với entity changes
4. Review migration trước khi merge PR

---

## 📞 Support

Nếu gặp vấn đề:

1. Xem logs: `mvn flyway:info`
2. Check database: `SELECT * FROM flyway_schema_history;`
3. Đọc documentation: [FLYWAY_CODE_FIRST_GUIDE.md](FLYWAY_CODE_FIRST_GUIDE.md)
4. Check Flyway docs: https://flywaydb.org/documentation/

---

**Happy Coding! 🎉**
