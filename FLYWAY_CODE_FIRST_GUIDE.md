# Hướng Dẫn Code-First với Flyway & PostgreSQL

## Tổng Quan

Dự án này sử dụng **Code-First approach** với Flyway để quản lý database schema:
- ✅ **Entities (Java)** là nguồn sự thật (source of truth)
- ✅ **JPA/Hibernate** generate DDL từ entities
- ✅ **Flyway migrations** được tạo từ DDL để version control schema
- ✅ **PostgreSQL** là database production

## Quy Trình Code-First

### 🔄 Workflow Chuẩn

```
1. Tạo/Sửa Entity (Java) 
   ↓
2. Hibernate Generate DDL (Automatic)
   ↓
3. Tạo Flyway Migration từ DDL (Manual/Script)
   ↓
4. Test Migration
   ↓
5. Commit vào Git
```

---

## 1️⃣ Bước 1: Cấu Hình Application

### File: `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smd_db
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: none  # Quan trọng: Không để Hibernate tự động thay đổi schema
    show-sql: true
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

**Lưu ý quan trọng:**
- `ddl-auto: none` → Hibernate KHÔNG tự động thay đổi DB
- `flyway.enabled: true` → Flyway quản lý toàn bộ schema changes

---

## 2️⃣ Bước 2: Tạo Baseline Migration (Lần đầu)

### Option A: Sử dụng Script Tự Động (Khuyến nghị)

```bash
# Bước 1: Generate schema từ Hibernate
mvn clean compile

# Bước 2: Tạm thời đổi ddl-auto để generate DDL
# Trong application.yaml, đổi:
#   ddl-auto: none -> ddl-auto: create

# Bước 3: Chạy app và capture DDL
# Hibernate sẽ in ra console toàn bộ CREATE TABLE statements

# Bước 4: Copy DDL vào file migration
# Tạo file: src/main/resources/db/migration/V1__Initial_Schema.sql

# Bước 5: Đổi lại ddl-auto: none
```

### Option B: Sử dụng Hibernate Schema Export Plugin

Thêm vào `pom.xml`:

```xml
<plugin>
    <groupId>org.hibernate.orm.tooling</groupId>
    <artifactId>hibernate-enhance-maven-plugin</artifactId>
    <version>${hibernate.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>enhance</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Chạy:
```bash
mvn hibernate-enhance:export
```

### Option C: Manual Export với JPA Schema Generation

Thêm vào `application.yaml` (temporary):

```yaml
spring:
  jpa:
    properties:
      jakarta.persistence.schema-generation.scripts.action: create
      jakarta.persistence.schema-generation.scripts.create-target: target/schema-create.sql
      jakarta.persistence.schema-generation.scripts.create-source: metadata
```

Sau đó run application, schema sẽ được export vào `target/schema-create.sql`

---

## 3️⃣ Bước 3: Tạo Migration Mới (Khi Thay Đổi Entity)

### Ví dụ: Thêm column mới vào Subject

**1. Sửa Entity:**

```java
@Entity
@Table(name = "subjects")
public class Subject {
    // ... existing fields
    
    @Column(name = "is_featured")
    Boolean isFeatured; // NEW FIELD
}
```

**2. Generate DDL Change:**

Cách 1: Manual (nhỏ) → Viết tay migration
Cách 2: Auto (lớn) → Dùng diff tool

**3. Tạo Migration File:**

```sql
-- File: V2__Add_Featured_Flag_To_Subject.sql
ALTER TABLE subjects 
ADD COLUMN is_featured BOOLEAN DEFAULT false;
```

**4. Test Migration:**

```bash
mvn flyway:migrate
```

---

## 4️⃣ Naming Convention cho Migrations

### Format chuẩn:
```
V{version}__{description}.sql
```

### Ví dụ:
```
V1__Initial_Schema.sql
V2__Add_Featured_Flag_To_Subject.sql
V3__Create_Index_On_Subject_Code.sql
V4__Add_Curriculum_Constraints.sql
V5__Alter_CLO_Mapping_Table.sql
```

### Quy tắc:
- ✅ Version: Số tự nhiên tăng dần (1, 2, 3...)
- ✅ Separator: Double underscore `__`
- ✅ Description: Snake_case, mô tả ngắn gọn
- ✅ Extension: `.sql`
- ❌ KHÔNG được sửa file migration đã apply!

---

## 5️⃣ Best Practices

### ✅ DO's

1. **Luôn test migration trước khi commit**
   ```bash
   mvn flyway:clean flyway:migrate
   ```

2. **Backup data trước khi migrate (production)**
   ```bash
   pg_dump smd_db > backup_$(date +%Y%m%d).sql
   ```

3. **Viết migration có thể rollback**
   ```sql
   -- V3__Add_Index.sql
   CREATE INDEX idx_subject_code ON subjects(subject_code);
   
   -- R3__Rollback_Add_Index.sql (Repeatable)
   DROP INDEX IF EXISTS idx_subject_code;
   ```

4. **Sử dụng transactions (mặc định trong Flyway)**

5. **Comment rõ ràng trong migration**
   ```sql
   -- Add index to improve query performance for subject lookup by code
   CREATE INDEX idx_subject_code ON subjects(subject_code);
   ```

### ❌ DON'Ts

1. ❌ KHÔNG sửa migration đã apply
2. ❌ KHÔNG dùng `ddl-auto: update` trong production
3. ❌ KHÔNG skip version number
4. ❌ KHÔNG commit migration chưa test

---

## 6️⃣ Troubleshooting

### Lỗi: "Validate failed: Migration checksum mismatch"

**Nguyên nhân:** File migration đã apply bị sửa đổi

**Giải pháp:**
```bash
# Option 1: Repair (nếu chắc chắn)
mvn flyway:repair

# Option 2: Clean và migrate lại (CHỈ trong dev)
mvn flyway:clean flyway:migrate
```

### Lỗi: Schema mismatch giữa Entities và DB

**Nguyên nhân:** Sửa entity nhưng chưa tạo migration

**Giải pháp:**
1. Revert entity về trạng thái cũ
2. Tạo migration trước
3. Apply migration
4. Sau đó mới sửa entity

### Lỗi: "Table already exists"

**Nguyên nhân:** DB đã có schema từ trước, Flyway chưa baseline

**Giải pháp:**
```bash
mvn flyway:baseline
mvn flyway:migrate
```

---

## 7️⃣ Commands Thường Dùng

```bash
# Xem trạng thái migrations
mvn flyway:info

# Apply pending migrations
mvn flyway:migrate

# Validate migrations
mvn flyway:validate

# Clean database (CHỈ dev)
mvn flyway:clean

# Baseline existing database
mvn flyway:baseline

# Repair metadata table
mvn flyway:repair
```

---

## 8️⃣ CI/CD Integration

### GitHub Actions Example

```yaml
name: Database Migration
on:
  push:
    paths:
      - 'src/main/resources/db/migration/**'

jobs:
  migrate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run Flyway Migrate
        run: mvn flyway:migrate
        env:
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
```

---

## 9️⃣ Advanced: Automatic Migration Generation

Để tự động generate migration từ entity changes, có thể dùng các công cụ:

### Option 1: Hibernate5DDLCommandLineExporter
### Option 2: Liquibase (alternative to Flyway)
### Option 3: Custom Script (xem file `generate-migration.sh`)

---

## 📚 Tài Liệu Tham Khảo

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Hibernate DDL Auto](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.using-hibernate)
- [PostgreSQL Best Practices](https://wiki.postgresql.org/wiki/Don%27t_Do_This)

---

## 📞 Support

Nếu có vấn đề, tham khảo:
1. Check `flyway_schema_history` table trong DB
2. Check application logs
3. Run `mvn flyway:info` để xem trạng thái
