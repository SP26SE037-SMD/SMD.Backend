# ========================================
# Stage 1: Build Stage
# ========================================
# Sử dụng Maven với Java 21 để build project
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Đặt working directory
WORKDIR /app

# Copy file pom.xml trước để tận dụng Docker layer caching
COPY pom.xml .

# Copy toàn bộ source code
COPY src src

# Build application (skip tests để build nhanh hơn)
RUN mvn clean package -DskipTests

# Kiểm tra file JAR đã được tạo
RUN ls -l /app/target

# ========================================
# Stage 2: Runtime Stage
# ========================================
# Sử dụng JDK 21 runtime image nhẹ hơn
FROM eclipse-temurin:21-jdk-jammy

# Đặt working directory
WORKDIR /app

# Copy file JAR từ build stage
COPY --from=build /app/target/smd-*.jar app.jar

# Expose port mà ứng dụng chạy (mặc định Spring Boot là 8080)
EXPOSE 8080

# Command to run the application
#ENTRYPOINT ["java", "-jar", "app.jar"] // Đang để mặc định, nên JVM đang chiếm nhiều RAM nen luu y

ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","app.jar"]
# '-Xms256m' _ Giới hạn RAM tối đa cho JVM để tránh chiếm quá nhiều tài nguyên trên máy chủ
#Khi app start → Java dùng 256MB RAM, nhưng trong quá trình chạy có thể tăng lên tối đa 512MB nếu cần thiết.
