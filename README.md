# 🍵 UTE TEA - Backend API

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)

**Backend API cho ứng dụng đặt trà sữa UTE Tea**

[Tính năng](#-tính-năng) • [Cài đặt](#️-cài-đặt-nhanh) • [API Docs](#-api-endpoints) • [Luồng hoạt động](#-luồng-hoạt-động-hệ-thống)

</div>

---

## 📖 Giới thiệu

**UTE Tea Backend** là REST API server được xây dựng bằng Spring Boot, phục vụ cho ứng dụng đặt trà sữa trực tuyến dành cho sinh viên và cộng đồng UTE.

### 🎯 Điểm nổi bật

- 🔐 **Bảo mật cao** - JWT Authentication + BCrypt password hashing
- 🚀 **Hiệu năng tốt** - Spring Boot 3.5.7 + JPA/Hibernate optimization
- 📱 **Mobile-friendly** - RESTful API chuẩn cho Android/iOS
- 📊 **Dashboard quản lý** - Thống kê doanh thu, đơn hàng real-time
- 🎟️ **Hệ thống khuyến mãi** - Mã giảm giá linh hoạt (%, fixed amount)
- 🗄️ **Database cloud** - MySQL trên Aiven Cloud (99.9% uptime)

---

## 🚀 Tính năng

### 👤 Dành cho Khách hàng

✅ Đăng ký/Đăng nhập tài khoản  
✅ Xem menu 16+ món nước (4 categories)  
✅ Tìm kiếm món theo tên  
✅ Chọn size (M, L, Jumbo) và topping  
✅ Đặt hàng online (Delivery/Pickup)  
✅ Áp dụng mã giảm giá  
✅ Theo dõi trạng thái đơn hàng  
✅ Xem lịch sử đơn hàng  

### 👨‍💼 Dành cho Quản lý

✅ Dashboard thống kê tổng quan  
✅ Quản lý đơn hàng (xem, cập nhật trạng thái)  
✅ Quản lý menu (thêm/sửa/xóa món)  
✅ Quản lý danh mục  
✅ Báo cáo doanh thu theo ngày/tháng  
✅ Xem top món bán chạy  

---

## ⚙️ Cài đặt nhanh

### Yêu cầu hệ thống

- ☕ Java JDK 17 trở lên
- 📦 Maven 3.6+ (hoặc dùng Maven Wrapper có sẵn)
- 🗄️ MySQL 8.0+ (tùy chọn - đã có cloud database)

### Bước 1: Clone project

```bash
git clone <repository-url>
cd backend_utetea
```

### Bước 2: Chạy server

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

### Bước 3: Kiểm tra

Server chạy tại: **http://localhost:8080**

Test API:
```bash
curl http://localhost:8080/api/auth/health
```

Kết quả mong đợi:
```json
{
  "success": true,
  "message": "API is running",
  "data": "OK"
}
```

✅ **Xong!** API đã sẵn sàng sử dụng.

> **Lưu ý:** Database cloud đã được cấu hình sẵn trong `application.properties`, không cần setup thêm!

---

## 📡 API Endpoints

### 🔓 Public (Không cần authentication)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/auth/register` | Đăng ký tài khoản |
| `POST` | `/api/auth/login` | Đăng nhập |
| `GET` | `/api/drinks` | Lấy danh sách món |
| `GET` | `/api/drinks/{id}` | Chi tiết món |
| `GET` | `/api/categories` | Lấy danh mục |
| `GET` | `/api/stores` | Lấy cửa hàng |
| `GET` | `/api/promotions` | Lấy mã giảm giá |

### 🔐 User (Cần JWT token)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/orders` | Tạo đơn hàng |
| `GET` | `/api/orders/user/{userId}` | Lịch sử đơn |
| `GET` | `/api/orders/{orderId}` | Chi tiết đơn |

### 👨‍💼 Manager (Chỉ MANAGER role)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/manager/summary` | Dashboard thống kê |
| `GET` | `/api/manager/orders` | Xem tất cả đơn |
| `PUT` | `/api/manager/orders/{id}/status` | Cập nhật trạng thái |
| `POST` | `/api/admin/drinks` | Thêm món mới |
| `PUT` | `/api/admin/drinks/{id}` | Sửa món |
| `DELETE` | `/api/admin/drinks/{id}` | Xóa món |

### 📚 API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## 🔄 Luồng hoạt động hệ thống

### 1️⃣ Authentication Flow

```
┌─────────────┐                                    ┌──────────────┐
│ Android App │                                    │ Backend API  │
└──────┬──────┘                                    └──────┬───────┘
       │                                                  │
       │  POST /api/auth/register                        │
       │  { username, password, fullName, phone }        │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │                                                  │  Validate input
       │                                                  │  Hash password (BCrypt)
       │                                                  │  Save to database
       │                                                  │
       │  { success: true, message: "Registered" }       │
       │<────────────────────────────────────────────────┤
       │                                                  │
       │  POST /api/auth/login                           │
       │  { usernameOrPhone, password }                  │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │                                                  │  Find user
       │                                                  │  Verify password
       │                                                  │  Generate JWT token
       │                                                  │
       │  { token, userId, username, role }              │
       │<────────────────────────────────────────────────┤
       │                                                  │
       │  GET /api/orders                                │
       │  Header: Authorization: Bearer <token>          │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │                                                  │  Validate JWT
       │                                                  │  Extract userId
       │                                                  │  Query database
       │                                                  │
       │  { orders: [...] }                              │
       │<────────────────────────────────────────────────┤
       │                                                  │
```

**Chi tiết:**
1. **Register:** Validate → Hash password (BCrypt) → Save DB → Return success
2. **Login:** Validate credentials → Generate JWT (24h) → Return token + user info
3. **Protected API:** Validate JWT → Extract user → Process request

---

### 2️⃣ Order Creation Flow

```
┌──────────┐     ┌────────────┐     ┌──────────┐     ┌──────────┐
│  Client  │     │ Controller │     │  Service │     │ Database │
└────┬─────┘     └─────┬──────┘     └────┬─────┘     └────┬─────┘
     │                 │                  │                │
     │ POST /api/orders│                  │                │
     ├────────────────>│                  │                │
     │                 │ createOrder()    │                │
     │                 ├─────────────────>│                │
     │                 │                  │ Validate user  │
     │                 │                  ├───────────────>│
     │                 │                  │<───────────────┤
     │                 │                  │ Validate store │
     │                 │                  ├───────────────>│
     │                 │                  │<───────────────┤
     │                 │                  │ Get drink info │
     │                 │                  ├───────────────>│
     │                 │                  │<───────────────┤
     │                 │                  │                │
     │                 │                  │ Calculate price│
     │                 │                  │ Apply promotion│
     │                 │                  │                │
     │                 │                  │ Save order     │
     │                 │                  ├───────────────>│
     │                 │                  │ Save items     │
     │                 │                  ├───────────────>│
     │                 │                  │<───────────────┤
     │                 │ OrderDto         │                │
     │                 │<─────────────────┤                │
     │ Success + Order │                  │                │
     │<────────────────┤                  │                │
     │                 │                  │                │
```

**Tính toán giá:**

```java
// Giá 1 món
itemPrice = (basePrice + sizeExtraPrice + sum(toppingPrices)) × quantity

// Tổng đơn hàng
subtotal = sum(itemPrice)

// Áp dụng giảm giá
if (promotionCode valid) {
    if (type == PERCENTAGE) {
        discount = subtotal × (value / 100)
        if (discount > maxDiscount) discount = maxDiscount
    } else {
        discount = value
    }
}

// Giá cuối cùng
finalPrice = subtotal - discount + shippingFee
```

---

### 3️⃣ Order Status Lifecycle

```
                    ┌─────────┐
                    │ PENDING │ ◄── Đơn hàng mới tạo
                    └────┬────┘
                         │
            ┌────────────┼────────────┐
            │                         │
            ▼                         ▼
      ┌──────────┐              ┌──────────┐
      │  MAKING  │              │ CANCELED │ ◄── Hủy đơn
      └────┬─────┘              └──────────┘
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
┌──────────┐  ┌────────┐
│ SHIPPING │  │ READY  │ ◄── Sẵn sàng lấy (Pickup)
└────┬─────┘  └───┬────┘
     │            │
     │            │
     ▼            ▼
   ┌────────────────┐
   │      DONE      │ ◄── Hoàn thành
   └────────────────┘
```

**Mô tả trạng thái:**

| Status | Mô tả | Có thể chuyển sang |
|--------|-------|-------------------|
| `PENDING` | Chờ xác nhận | MAKING, CANCELED |
| `MAKING` | Đang pha chế | SHIPPING, READY, CANCELED |
| `SHIPPING` | Đang giao hàng (Delivery) | DONE, CANCELED |
| `READY` | Sẵn sàng lấy (Pickup) | DONE, CANCELED |
| `DONE` | Hoàn thành | - |
| `CANCELED` | Đã hủy | - |

---

### 4️⃣ Promotion Validation Flow

```
                    ┌──────────────────┐
                    │ Apply Promo Code │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ Code exists?     │
                    └────┬────────┬────┘
                         │ No     │ Yes
                         ▼        ▼
                   ┌─────────┐  ┌──────────┐
                   │ Invalid │  │ Active?  │
                   └─────────┘  └────┬─────┘
                                     │ Yes
                                     ▼
                              ┌──────────────┐
                              │ Check dates  │
                              └──────┬───────┘
                                     │ Valid
                                     ▼
                              ┌──────────────┐
                              │ Min order?   │
                              └──────┬───────┘
                                     │ OK
                                     ▼
                              ┌──────────────┐
                              │ Usage limit? │
                              └──────┬───────┘
                                     │ OK
                                     ▼
                              ┌──────────────┐
                              │ Apply Discount│
                              └──────────────┘
```

**Validation Rules:**

```java
// Kiểm tra mã giảm giá
if (!promotion.isActive) 
    return "Mã không còn hiệu lực";
    
if (today < promotion.startDate) 
    return "Mã chưa bắt đầu";
    
if (today > promotion.endDate) 
    return "Mã đã hết hạn";
    
if (orderTotal < promotion.minOrderAmount) 
    return "Đơn hàng chưa đủ điều kiện";
    
if (promotion.usageCount >= promotion.maxUsage) 
    return "Mã đã hết lượt sử dụng";

// Áp dụng giảm giá
if (promotion.type == PERCENTAGE) {
    discount = orderTotal × (promotion.value / 100);
    if (discount > promotion.maxDiscount) {
        discount = promotion.maxDiscount;
    }
} else {
    discount = promotion.value;
}
```

---

## 💾 Cấu trúc Database

### Entity Relationship Diagram

```
┌──────────────┐
│    users     │
│──────────────│
│ id (PK)      │
│ username     │
│ password     │
│ role         │
│ memberTier   │
└──────┬───────┘
       │ 1
       │
       │ N
┌──────▼───────┐      N ┌──────────────┐
│   orders     │◄───────┤  promotions  │
│──────────────│        │──────────────│
│ id (PK)      │        │ code         │
│ userId (FK)  │        │ type         │
│ storeId (FK) │        │ value        │
│ status       │        └──────────────┘
│ totalPrice   │
└──────┬───────┘
       │ 1
       │
       │ N
┌──────▼───────┐
│ order_items  │
│──────────────│
│ id (PK)      │
│ orderId (FK) │
│ drinkId (FK) │
│ quantity     │
└──────┬───────┘
       │ 1
       │
       │ N
┌──────▼────────────┐
│ order_item_       │
│   toppings        │
│───────────────────│
│ orderItemId (FK)  │
│ toppingName       │
└───────────────────┘

┌──────────────────┐
│ drink_categories │
│──────────────────│
│ id (PK)          │
│ name             │
└────────┬─────────┘
         │ 1
         │
         │ N
    ┌────▼─────┐
    │  drinks  │
    │──────────│
    │ id (PK)  │
    │ name     │
    │ price    │
    └────┬─────┘
         │ 1
    ┌────┴─────┬────────────┐
    │ N        │ N          │
┌───▼──────┐ ┌─▼──────────┐│
│drink_    │ │drink_      ││
│sizes     │ │toppings    ││
└──────────┘ └────────────┘│
```

### Dữ liệu mẫu

#### 🔑 Tài khoản test

| Username | Password | Role | Member Tier |
|----------|----------|------|-------------|
| `manager_ute` | `123456` | MANAGER | - |
| `ute_student_01` | `123456` | USER | BRONZE |
| `ute_student_02` | `123456` | USER | SILVER |
| `ute_student_03` | `123456` | USER | GOLD |

#### 🥤 Categories & Drinks

| Category | Số món | Ví dụ |
|----------|--------|-------|
| **Milk Tea** | 4 | Trà sữa Houjicha Classic, Trà sữa Oolong |
| **Fruit Tea** | 5 | Đào hồng UTE, Trà vải nhai tươi |
| **Macchiato** | 3 | Trà kem cheese, Macchiato caramel |
| **Special** | 4 | Aiyu jelly, Trà long nhãn |

#### 🎟️ Mã giảm giá

| Code | Loại | Giá trị | Đơn tối thiểu |
|------|------|---------|---------------|
| `STUDENT20` | Percentage | 20% | 50,000đ |
| `FREESHIPUTE` | Fixed | 15,000đ | 60,000đ |
| `COMBO4UTE` | Fixed | 30,000đ | 120,000đ |

---

## 🧪 Testing & Examples

### Test với cURL

#### 1. Health Check
```bash
curl http://localhost:8080/api/auth/health
```

#### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrPhone": "ute_student_01",
    "password": "123456"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 2,
    "username": "ute_student_01",
    "fullName": "Nguyen Thi A",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### 3. Get Menu
```bash
curl http://localhost:8080/api/drinks
```

#### 4. Create Order (với JWT)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": 2,
    "storeId": 1,
    "type": "DELIVERY",
    "address": "KTX khu A, UTE",
    "paymentMethod": "COD",
    "promotionCode": "STUDENT20",
    "items": [
      {
        "drinkId": 1,
        "sizeName": "L",
        "quantity": 2,
        "note": "Ít đường",
        "toppings": [
          { "toppingName": "Trân châu đen" }
        ]
      }
    ]
  }'
```

### Test với Swagger UI

1. Mở http://localhost:8080/swagger-ui.html
2. Login để lấy token
3. Click **Authorize**, nhập: `Bearer <token>`
4. Test các endpoints

---

## 🏗️ Kiến trúc Project

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         Controller Layer                │  ← REST API Endpoints
│  (AuthController, DrinkController, ...) │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Service Layer                  │  ← Business Logic
│  (AuthService, DrinkService, ...)      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Repository Layer                 │  ← Data Access (JPA)
│  (UserRepository, DrinkRepository, ...) │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           Database (MySQL)              │  ← Data Storage
└─────────────────────────────────────────┘
```

### Cấu trúc thư mục

```
src/main/java/com/utetea/backend/
├── 📁 config/              # Cấu hình (Security, Swagger, CORS)
├── 📁 controller/          # REST API Controllers
├── 📁 service/             # Business Logic
├── 📁 repository/          # Data Access Layer (JPA)
├── 📁 model/               # Entity Models (Database Tables)
├── 📁 dto/                 # Data Transfer Objects
├── 📁 security/            # Security & JWT
├── 📁 exception/           # Exception Handling
└── 📁 mapper/              # Entity ↔ DTO Mappers
```

---

## 🔒 Security

### Authentication

- **JWT Token:** Expiration 24 hours
- **Password:** BCrypt hashing (cost factor: 10)
- **Authorization:** Role-based (USER, MANAGER)

### CORS Configuration

```java
// Cho phép tất cả origins (Development)
// Production nên giới hạn specific domains
allowedOrigins: "*"
allowedMethods: GET, POST, PUT, DELETE
```

---

## 📱 Kết nối với Android App

### Emulator
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/api/"
```

### Device thật
```kotlin
// Thay YOUR_IP bằng IP máy tính (VD: 192.168.1.100)
private const val BASE_URL = "http://YOUR_IP:8080/api/"
```

**Lấy IP máy tính:**
```bash
# Windows
ipconfig

# Mac/Linux
ifconfig
```

---

## 🛠️ Công nghệ sử dụng

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Java | 17 | Programming language |
| Spring Boot | 3.5.7 | Application framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.x | Database ORM |
| MySQL | 8.0 | Relational database |
| JWT | 0.11.5 | Token-based auth |
| Swagger/OpenAPI | 3.0 | API documentation |
| Lombok | Latest | Reduce boilerplate |
| Maven | 3.6+ | Build tool |

---

## 🐛 Troubleshooting

### Port 8080 đã được sử dụng
```properties
# Thêm vào application.properties
server.port=8081
```

### Java version không đúng
```bash
java -version  # Phải >= 17
```
Cài Java 17: https://adoptium.net/

### Maven command not found
Dùng Maven Wrapper:
```bash
.\mvnw.cmd spring-boot:run  # Windows
./mvnw spring-boot:run      # Mac/Linux
```

---

## 📦 Build & Deploy

### Build JAR file
```bash
.\mvnw.cmd clean package
```

File JAR: `target/backend-0.0.1-SNAPSHOT.jar`

### Chạy JAR
```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## 📄 License

MIT License

---

## 👥 Team

**Đồ án Lập trình Di động - UTE Tea**

- Backend API: Spring Boot + MySQL
- Android App: Java/Kotlin
- Database: MySQL 8.0

---

<div align="center">

**🍵 UTE Tea - Trà sữa cho sinh viên UTE 🍵**

Made with ❤️ by UTE Students

</div>
