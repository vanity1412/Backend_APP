# 🚀 HƯỚNG DẪN CHẠY API - UTE TEA BACKEND

## 📋 Mục lục
1. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
2. [Cài đặt môi trường](#cài-đặt-môi-trường)
3. [Cấu hình Database](#cấu-hình-database)
4. [Chạy ứng dụng](#chạy-ứng-dụng)
5. [Test API](#test-api)
6. [Kết nối từ Android](#kết-nối-từ-android)
7. [Troubleshooting](#troubleshooting)

---

## 🖥️ Yêu cầu hệ thống

### Phần mềm cần thiết:
- ✅ **Java JDK 17** trở lên
- ✅ **Maven 3.6+** (hoặc dùng Maven Wrapper có sẵn trong project)
- ✅ **MySQL 8.0+** (hoặc sử dụng database cloud đã cấu hình)
- ✅ **Git** (để clone project)
- ✅ **IDE**: IntelliJ IDEA, Eclipse, hoặc VS Code (tùy chọn)

### Kiểm tra phiên bản đã cài:
```bash
# Kiểm tra Java
java -version
# Output mong muốn: java version "17.x.x" hoặc cao hơn

# Kiểm tra Maven (nếu cài global)
mvn -version
# Output mong muốn: Apache Maven 3.6.x hoặc cao hơn

# Kiểm tra MySQL
mysql --version
# Output mong muốn: mysql Ver 8.0.x
```

---

## 🔧 Cài đặt môi trường

### Bước 1: Clone hoặc mở project
```bash
# Nếu clone từ Git
git clone <repository-url>
cd backend_utetea

# Hoặc mở folder project có sẵn
cd path/to/backend_utetea
```

### Bước 2: Cài đặt dependencies
```bash
# Windows - Sử dụng Maven Wrapper (KHUYẾN NGHỊ)
.\mvnw.cmd clean install

# Mac/Linux - Sử dụng Maven Wrapper
./mvnw clean install

# Hoặc dùng Maven global (nếu đã cài)
mvn clean install
```

**Lưu ý:** Lần đầu chạy sẽ mất thời gian để download dependencies.

---

## 💾 Cấu hình Database

### Option 1: Sử dụng Database Cloud (ĐÃ CẤU HÌNH SẴN)

Project đã được cấu hình sẵn với database cloud Aiven MySQL. Bạn **KHÔNG CẦN** cài MySQL local.

File `src/main/resources/application.properties` đã có:
```properties
spring.datasource.url=jdbc:mysql://mysql-16b47c6b-phongtran080809-7c70.c.aivencloud.com:26260/LTDD_Thong?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=avnadmin
spring.datasource.password=AVNS_Ix83Fzpvp1FUIgDMvry
```

✅ **Database đã có sẵn dữ liệu mẫu**, bạn có thể chạy ngay!

---

### Option 2: Sử dụng MySQL Local (TÙY CHỌN)

Nếu muốn dùng MySQL local:

#### Bước 1: Tạo Database
```sql
CREATE DATABASE IF NOT EXISTS LTDD_Thongtesst 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE LTDD_Thongtesst;
```

#### Bước 2: Tạo bảng drink_categories
Chạy file: `src/main/resources/schema-categories.sql`

```sql
-- Hoặc copy-paste SQL này:
CREATE TABLE IF NOT EXISTS drink_categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    display_order INT DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Bước 3: Import dữ liệu mẫu
Chạy file: `src/main/resources/data-ltdd.sql`

```bash
# Cách 1: Dùng MySQL Workbench
# - Mở file data-ltdd.sql
# - Execute toàn bộ script

# Cách 2: Dùng command line
mysql -u root -p LTDD_Thongtesst < src/main/resources/data-ltdd.sql
```

#### Bước 4: Cập nhật application.properties
```properties
# Thay đổi connection string
spring.datasource.url=jdbc:mysql://localhost:3306/LTDD_Thongtesst?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

---

## 🏃 Chạy ứng dụng

### Cách 1: Dùng Maven Wrapper (KHUYẾN NGHỊ)
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

### Cách 2: Dùng Maven global
```bash
mvn spring-boot:run
```

### Cách 3: Chạy từ IDE

#### IntelliJ IDEA:
1. Mở project trong IntelliJ
2. Tìm file `src/main/java/com/utetea/backend/BackendApplication.java`
3. Click chuột phải → **Run 'BackendApplication'**

#### Eclipse:
1. Import project as Maven project
2. Tìm file `BackendApplication.java`
3. Right click → **Run As** → **Java Application**

#### VS Code:
1. Cài extension: **Spring Boot Extension Pack**
2. Mở file `BackendApplication.java`
3. Click **Run** ở trên hàm `main()`

---

### ✅ Kiểm tra server đã chạy thành công

Khi server chạy thành công, bạn sẽ thấy log:
```
Started BackendApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

Truy cập: **http://localhost:8080**

---

## 🧪 Test API

### 1. Test bằng Browser (Đơn giản nhất)

Mở browser và truy cập:
```
http://localhost:8080/api/drinks
http://localhost:8080/api/categories
http://localhost:8080/api/stores
http://localhost:8080/api/promotions
```

Bạn sẽ thấy JSON response với dữ liệu.

---

### 2. Test bằng Swagger UI (KHUYẾN NGHỊ)

Swagger UI cung cấp giao diện web để test tất cả API endpoints.

**Truy cập:** http://localhost:8080/swagger-ui.html

#### Cách sử dụng Swagger:
1. Mở http://localhost:8080/swagger-ui.html
2. Chọn endpoint muốn test (ví dụ: **GET /api/drinks**)
3. Click **Try it out**
4. Click **Execute**
5. Xem Response

#### Test với Authentication:
1. Login để lấy token:
   - Mở endpoint **POST /api/auth/login**
   - Click **Try it out**
   - Nhập:
     ```json
     {
       "usernameOrPhone": "ute_student_01",
       "password": "123456"
     }
     ```
   - Click **Execute**
   - Copy **token** từ response

2. Authorize:
   - Click nút **Authorize** ở đầu trang
   - Nhập: `Bearer <token-vừa-copy>`
   - Click **Authorize**

3. Giờ bạn có thể test các API cần authentication!

---

### 3. Test bằng Postman

#### Import Collection:
1. Mở Postman
2. Import → Raw text
3. Paste URL: http://localhost:8080/v3/api-docs
4. Postman sẽ tự động tạo collection

#### Test Login:
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

Body:
{
  "usernameOrPhone": "ute_student_01",
  "password": "123456"
}
```

#### Test Get Drinks:
```
GET http://localhost:8080/api/drinks
```

---

### 4. Test bằng cURL (Command Line)

```bash
# Test health check
curl http://localhost:8080/api/auth/health

# Test get drinks
curl http://localhost:8080/api/drinks

# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"usernameOrPhone\":\"ute_student_01\",\"password\":\"123456\"}"

# Test get categories
curl http://localhost:8080/api/categories
```

---

## 📱 Kết nối từ Android

### Bước 1: Tìm IP máy tính

#### Windows:
```bash
ipconfig
# Tìm dòng "IPv4 Address" trong phần WiFi adapter
# Ví dụ: 192.168.1.100
```

#### Mac/Linux:
```bash
ifconfig
# Hoặc
ip addr show
```

### Bước 2: Cấu hình Firewall

#### Windows:
1. Mở **Windows Defender Firewall**
2. **Advanced settings** → **Inbound Rules**
3. **New Rule** → **Port** → **TCP** → **8080**
4. **Allow the connection**

#### Mac:
```bash
# Mac thường không cần cấu hình firewall cho local network
```

### Bước 3: Test từ điện thoại

Đảm bảo điện thoại và máy tính **cùng WiFi**.

Mở browser trên điện thoại:
```
http://192.168.1.100:8080/api/drinks
```
(Thay `192.168.1.100` bằng IP máy tính của bạn)

### Bước 4: Cấu hình trong Android App

```java
// Retrofit Base URL
public static final String BASE_URL = "http://192.168.1.100:8080/";

// Hoặc dùng biến môi trường
// Development: http://192.168.1.100:8080/
// Production: https://api.utetea.com/
```

**Lưu ý:** Thêm permission trong `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Cho phép HTTP (không phải HTTPS) trong development -->
<application
    android:usesCleartextTraffic="true"
    ...>
```

---

## 🐛 Troubleshooting

### ❌ Lỗi: Port 8080 đã được sử dụng

**Giải pháp 1:** Đổi port
```properties
# File: application.properties
server.port=8081
```

**Giải pháp 2:** Tắt process đang dùng port 8080
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :8080
kill -9 <PID>
```

---

### ❌ Lỗi: Không kết nối được MySQL

**Kiểm tra:**
1. MySQL service đã chạy chưa?
   ```bash
   # Windows
   services.msc → Tìm MySQL → Start
   
   # Mac
   brew services start mysql
   
   # Linux
   sudo systemctl start mysql
   ```

2. Username/password đúng chưa?
3. Database `LTDD_Thongtesst` đã tạo chưa?

**Giải pháp:** Dùng database cloud (đã cấu hình sẵn)

---

### ❌ Lỗi: Java version không đúng

```bash
# Kiểm tra version
java -version

# Nếu < 17, cài Java 17:
# Windows: Download từ https://adoptium.net/
# Mac: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk
```

---

### ❌ Lỗi: Maven command not found

**Giải pháp:** Dùng Maven Wrapper (có sẵn trong project)
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

---

### ❌ Lỗi: Cannot resolve dependencies

```bash
# Xóa cache Maven và build lại
.\mvnw.cmd clean install -U

# Hoặc xóa folder .m2 cache
# Windows: C:\Users\<username>\.m2\repository
# Mac/Linux: ~/.m2/repository
```

---

### ❌ Lỗi: Access denied for user

**Nguyên nhân:** Sai username/password MySQL

**Giải pháp:**
```properties
# Kiểm tra lại application.properties
spring.datasource.username=root
spring.datasource.password=your_correct_password
```

---

### ❌ Lỗi: Table doesn't exist

**Nguyên nhân:** Chưa chạy SQL scripts

**Giải pháp:**
1. Chạy `schema-categories.sql` trước
2. Chạy `data-ltdd.sql` sau

Hoặc để Hibernate tự tạo:
```properties
spring.jpa.hibernate.ddl-auto=update
```

---

### ❌ Không kết nối được từ điện thoại

**Kiểm tra:**
1. Điện thoại và máy tính cùng WiFi?
2. Firewall đã mở port 8080?
3. IP máy tính đúng chưa?
4. Server đang chạy?

**Test:**
```bash
# Trên máy tính, test:
curl http://localhost:8080/api/drinks

# Trên điện thoại, test:
http://<IP-may-tinh>:8080/api/drinks
```

---

## 📊 Dữ liệu mẫu có sẵn

### Users (Tài khoản test):
| Username | Password | Role | Member Tier |
|----------|----------|------|-------------|
| manager_ute | 123456 | MANAGER | - |
| ute_student_01 | 123456 | USER | BRONZE |
| ute_student_02 | 123456 | USER | SILVER |
| ute_student_03 | 123456 | USER | GOLD |

### Categories:
- Milk Tea (4 món)
- Fruit Tea (5 món)
- Macchiato (3 món)
- Special (4 món)

### Promotions:
- **STUDENT20**: Giảm 20% (đơn tối thiểu 50,000đ)
- **FREESHIPUTE**: Giảm 15,000đ ship (đơn tối thiểu 60,000đ)
- **COMBO4UTE**: Giảm 30,000đ (đơn tối thiểu 120,000đ)

### Stores:
- UTE Tea - Cơ sở 1: Số 1 Võ Văn Ngân
- UTE Tea - Cơ sở 2: Khu KTX UTE

---

## 🎯 Quick Start Checklist

- [ ] Java 17 đã cài
- [ ] Project đã clone/mở
- [ ] Dependencies đã install (`mvnw clean install`)
- [ ] Database đã cấu hình (cloud hoặc local)
- [ ] Server chạy thành công (`mvnw spring-boot:run`)
- [ ] Test API bằng browser/Swagger
- [ ] Kết nối từ Android thành công

---

## 📚 Tài liệu tham khảo thêm

- **API Documentation:** `API-DOCUMENTATION.md`
- **Setup Instructions:** `SETUP-INSTRUCTIONS.md`
- **Database Guide:** `DATABASE-GUIDE.md` (file này)
- **Quick Start:** `QUICK-START.txt`

---

## 🆘 Cần hỗ trợ?

Nếu gặp vấn đề không có trong Troubleshooting:
1. Kiểm tra logs trong console
2. Kiểm tra file `application.properties`
3. Đảm bảo tất cả dependencies đã download
4. Restart IDE và thử lại

---

**Chúc bạn chạy API thành công! 🎉**

---

*Last updated: November 27, 2025*
