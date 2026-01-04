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

✅ **Xác thực & Bảo mật**
- Đăng ký tài khoản với OTP verification
- Đăng nhập JWT với refresh token
- Quên mật khẩu qua email OTP
- Đổi mật khẩu bảo mật

✅ **Quản lý Profile**
- Cập nhật thông tin cá nhân
- Upload avatar
- Xem điểm thưởng và hạng thành viên
- Xóa tài khoản (soft delete)

✅ **Mua sắm & Đặt hàng**
- Xem menu 16+ món nước (4 categories)
- Tìm kiếm món theo tên với input sanitization
- Chọn size (M, L, Jumbo) và topping
- Giỏ hàng cá nhân với real-time updates
- Đặt hàng online (Delivery/Pickup)
- Áp dụng mã giảm giá và member tier discount
- Theo dõi trạng thái đơn hàng real-time

✅ **Tính năng Đặc biệt**
- **Đặt hàng nhóm**: Tạo phiên đặt hàng với mã mời
- **Live Chat**: Hỗ trợ khách hàng real-time
- **Loyalty System**: Tích điểm, vòng xoay may mắn
- **Member Tiers**: Bronze/Silver/Gold/Diamond với quyền lợi

### 👨‍💼 Dành cho Manager

✅ **Quản lý Đơn hàng**
- Dashboard thống kê tổng quan
- Xem và cập nhật trạng thái đơn hàng
- Quản lý đơn hàng nhóm
- Báo cáo doanh thu chi tiết

✅ **Quản lý Sản phẩm**
- Quản lý menu (thêm/sửa/xóa món)
- Quản lý danh mục và sizes/toppings
- Xem top món bán chạy

✅ **Khuyến mãi & Marketing**
- Tạo và quản lý voucher/promotion
- Thiết lập điều kiện áp dụng
- Theo dõi usage statistics

✅ **Hỗ trợ Khách hàng**
- Live chat management
- Xử lý cuộc hội thoại chờ
- Phân công theo store

### 🛡️ Dành cho Admin

✅ **User Monitoring System**
- Dashboard giám sát tổng quan
- Theo dõi hoạt động người dùng
- Tính toán risk score tự động
- Hệ thống cảnh báo thông minh
- Quản lý IP bị chặn
- Xử lý tài khoản vi phạm

✅ **Bảo mật & Kiểm soát**
- Rate limiting configuration
- Blocked IP management
- Activity logging và audit trails
- Risk assessment và alerts  

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
| `POST` | `/api/auth/register-with-otp` | Đăng ký với OTP verification |
| `POST` | `/api/auth/otp-verify` | Xác thực OTP |
| `POST` | `/api/auth/resend-otp` | Gửi lại OTP |
| `POST` | `/api/auth/login` | Đăng nhập |
| `POST` | `/api/auth/forgot-password` | Quên mật khẩu |
| `POST` | `/api/auth/reset-password` | Reset mật khẩu |
| `POST` | `/api/auth/refresh-token` | Làm mới token |
| `GET` | `/api/auth/health` | Health check |
| `GET` | `/api/drinks` | Lấy danh sách món |
| `GET` | `/api/drinks/page` | Danh sách có phân trang |
| `GET` | `/api/drinks/{id}` | Chi tiết món |
| `GET` | `/api/drinks/search` | Tìm kiếm món |
| `GET` | `/api/promotions` | Lấy voucher active |
| `GET` | `/api/promotions/{id}` | Chi tiết voucher |
| `GET` | `/api/promotions/validate` | Validate mã voucher |

### 🔐 User (Cần JWT token)

#### 📦 Quản lý Đơn hàng
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/orders` | Tạo đơn hàng |
| `GET` | `/api/orders/my` | Đơn hàng của tôi |
| `GET` | `/api/orders/my/current` | Đơn đang xử lý |
| `GET` | `/api/orders/{orderId}` | Chi tiết đơn |

#### 🛒 Giỏ hàng
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/cart/add` | Thêm vào giỏ |
| `GET` | `/api/cart` | Xem giỏ hàng |
| `PUT` | `/api/cart/items/{cartItemId}` | Cập nhật số lượng |
| `DELETE` | `/api/cart/items/{cartItemId}` | Xóa sản phẩm |
| `DELETE` | `/api/cart/clear` | Xóa toàn bộ giỏ |

#### 👤 Profile
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/me` | Thông tin profile |
| `PUT` | `/api/me` | Cập nhật profile |
| `POST` | `/api/me/avatar` | Upload avatar |
| `PUT` | `/api/me/change-password` | Đổi mật khẩu |
| `DELETE` | `/api/me` | Xóa tài khoản |

#### 👥 Đặt hàng nhóm
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/group-orders` | Tạo phiên nhóm |
| `POST` | `/api/group-orders/join` | Tham gia bằng mã |
| `GET` | `/api/group-orders/{id}` | Thông tin phiên |
| `GET` | `/api/group-orders/code/{inviteCode}` | Tìm theo mã mời |
| `GET` | `/api/group-orders/active` | Phiên đang hoạt động |
| `POST` | `/api/group-orders/{id}/items` | Thêm món |
| `POST` | `/api/group-orders/{id}/checkout` | Thanh toán nhóm |

#### 💬 Live Chat
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/api/chat/conversations` | Bắt đầu chat |
| `POST` | `/api/chat/messages` | Gửi tin nhắn |
| `GET` | `/api/chat/conversations/my` | Cuộc hội thoại |

#### 🏆 Loyalty & Rewards
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/loyalty/points` | Điểm thưởng |
| `POST` | `/api/loyalty/spin` | Quay vòng may mắn |
| `GET` | `/api/loyalty/rewards` | Voucher chưa dùng |
| `GET` | `/api/loyalty/tier/benefits` | Quyền lợi tier |

### 👨‍💼 Manager (Chỉ MANAGER role)

#### 📊 Quản lý Đơn hàng
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/orders/all` | Tất cả đơn hàng |
| `PUT` | `/api/orders/{orderId}/status` | Cập nhật trạng thái |
| `GET` | `/api/orders/user/{userId}` | Đơn của user |

#### 🎁 Quản lý Khuyến mãi
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/promotions/manager/all` | Tất cả voucher |
| `POST` | `/api/promotions/manager` | Tạo voucher |
| `PUT` | `/api/promotions/manager/{id}` | Cập nhật voucher |
| `DELETE` | `/api/promotions/manager/{id}` | Xóa voucher |
| `PATCH` | `/api/promotions/manager/{id}/toggle-status` | Bật/tắt voucher |

#### 💬 Live Chat Management
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/chat/manager/conversations` | Cuộc hội thoại |
| `GET` | `/api/chat/manager/conversations/waiting-count` | Đếm chờ xử lý |
| `POST` | `/api/chat/conversations/{id}/close` | Đóng chat |

### 🛡️ Admin (Chỉ ADMIN role)

#### 🔍 User Monitoring
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/api/monitoring/dashboard` | Dashboard giám sát |
| `GET` | `/api/monitoring/activities` | Log hoạt động |
| `GET` | `/api/monitoring/activities/user/{userId}` | Log của user |
| `GET` | `/api/monitoring/alerts` | Danh sách cảnh báo |
| `GET` | `/api/monitoring/alerts/pending` | Cảnh báo chờ |
| `PUT` | `/api/monitoring/alerts/{alertId}/handle` | Xử lý cảnh báo |
| `GET` | `/api/monitoring/risk-scores` | Điểm rủi ro users |
| `POST` | `/api/monitoring/risk-scores/user/{userId}/reset` | Reset điểm rủi ro |
| `POST` | `/api/monitoring/users/{userId}/unblock` | Mở khóa user |

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

### Enhanced Entity Relationship Diagram

```
┌──────────────┐
│    users     │
│──────────────│
│ id (PK)      │
│ username     │
│ password     │
│ role         │
│ memberTier   │
│ points       │
│ riskScore    │
│ isBlocked    │
│ otp          │
│ otpExpiry    │
│ avatarUrl    │
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
│ status       │        │ startDate    │
│ totalPrice   │        │ endDate      │
│ discount     │        │ minOrderValue│
│ finalPrice   │        │ maxDiscount  │
│ type         │        │ usageLimit   │
│ address      │        │ usedCount    │
│ pickupTime   │        │ isActive     │
└──────┬───────┘        └──────────────┘
       │ 1
       │
       │ N
┌──────▼───────┐
│ order_items  │
│──────────────│
│ id (PK)      │
│ orderId (FK) │
│ drinkId (FK) │
│ sizeName     │
│ quantity     │
│ unitPrice    │
│ totalPrice   │
│ note         │
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
│ toppingPrice      │
└───────────────────┘

┌──────────────────┐
│ drink_categories │
│──────────────────│
│ id (PK)          │
│ name             │
│ description      │
│ imageUrl         │
└────────┬─────────┘
         │ 1
         │
         │ N
    ┌────▼─────┐
    │  drinks  │
    │──────────│
    │ id (PK)  │
    │ name     │
    │ basePrice│
    │ imageUrl │
    │ isActive │
    └────┬─────┘
         │ 1
    ┌────┴─────┬────────────┐
    │ N        │ N          │
┌───▼──────┐ ┌─▼──────────┐│
│drink_    │ │drink_      ││
│sizes     │ │toppings    ││
│──────────│ │────────────││
│sizeName  │ │name        ││
│extraPrice│ │price       ││
└──────────┘ └────────────┘│

┌─────────────────┐
│ group_orders    │
│─────────────────│
│ id (PK)         │
│ hostUserId (FK) │
│ inviteCode      │
│ status          │
│ isLocked        │
│ createdAt       │
│ expiresAt       │
└─────────┬───────┘
          │ 1
          │
          │ N
    ┌─────▼──────────┐
    │group_order_    │
    │   items        │
    │────────────────│
    │ id (PK)        │
    │ groupOrderId   │
    │ userId (FK)    │
    │ drinkId (FK)   │
    │ quantity       │
    └────────────────┘

┌─────────────────┐
│ conversations   │
│─────────────────│
│ id (PK)         │
│ userId (FK)     │
│ managerId (FK)  │
│ status          │
│ createdAt       │
└─────────┬───────┘
          │ 1
          │
          │ N
    ┌─────▼──────────┐
    │   messages     │
    │────────────────│
    │ id (PK)        │
    │ conversationId │
    │ senderId (FK)  │
    │ content        │
    │ timestamp      │
    └────────────────┘

┌─────────────────┐
│ user_activities │
│─────────────────│
│ id (PK)         │
│ userId (FK)     │
│ activityType    │
│ description     │
│ ipAddress       │
│ userAgent       │
│ timestamp       │
│ riskLevel       │
└─────────────────┘

┌─────────────────┐
│ monitoring_     │
│   alerts        │
│─────────────────│
│ id (PK)         │
│ userId (FK)     │
│ alertType       │
│ severity        │
│ message         │
│ isHandled       │
│ handledBy       │
│ createdAt       │
└─────────────────┘

┌─────────────────┐
│ loyalty_rewards │
│─────────────────│
│ id (PK)         │
│ userId (FK)     │
│ rewardType      │
│ voucherCode     │
│ isUsed          │
│ earnedAt        │
│ usedAt          │
└─────────────────┘
```

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

| Username | Password | Role | Member Tier | Points |
|----------|----------|------|-------------|--------|
| `manager_ute` | `123456` | MANAGER | - | - |
| `admin_ute` | `123456` | ADMIN | - | - |
| `ute_student_01` | `123456` | USER | BRONZE | 150 |
| `ute_student_02` | `123456` | USER | SILVER | 750 |
| `ute_student_03` | `123456` | USER | GOLD | 2500 |
| `ute_student_04` | `123456` | USER | DIAMOND | 8000 |

#### 🥤 Categories & Drinks

| Category | Số món | Ví dụ | Giá từ |
|----------|--------|-------|--------|
| **Milk Tea** | 4 | Trà sữa Houjicha Classic, Trà sữa Oolong | 35,000đ |
| **Fruit Tea** | 5 | Đào hồng UTE, Trà vải nhai tươi | 30,000đ |
| **Macchiato** | 3 | Trà kem cheese, Macchiato caramel | 40,000đ |
| **Special** | 4 | Aiyu jelly, Trà long nhãn | 45,000đ |

#### 🎟️ Mã giảm giá

| Code | Loại | Giá trị | Đơn tối thiểu | Giảm tối đa | Trạng thái |
|------|------|---------|---------------|-------------|------------|
| `STUDENT20` | Percentage | 20% | 50,000đ | 30,000đ | Active |
| `FREESHIPUTE` | Fixed | 15,000đ | 60,000đ | - | Active |
| `COMBO4UTE` | Fixed | 30,000đ | 120,000đ | - | Active |
| `NEWUSER50` | Percentage | 50% | 40,000đ | 25,000đ | Active |

#### 🏆 Member Tier Benefits

| Tier | Điều kiện | Discount | Spin Wheel | Bonus Points |
|------|-----------|----------|------------|--------------|
| **BRONZE** | 0 - 499 points | 0% | 1 lần/ngày | 1x |
| **SILVER** | 500 - 1999 points | 5% | 2 lần/ngày | 1.2x |
| **GOLD** | 2000 - 4999 points | 10% | 3 lần/ngày | 1.5x |
| **DIAMOND** | 5000+ points | 15% | 5 lần/ngày | 2x |

---

## 🎯 Business Logic & Advanced Features

### 💰 Pricing & Discount System

#### Multi-tier Discount Calculation
```java
// Tính giá cuối cùng với nhiều lớp giảm giá
public BigDecimal calculateFinalPrice(Order order, User user, Promotion promotion) {
    BigDecimal subtotal = calculateSubtotal(order.getItems());
    
    // 1. Áp dụng voucher discount
    BigDecimal voucherDiscount = BigDecimal.ZERO;
    if (promotion != null) {
        voucherDiscount = calculateVoucherDiscount(subtotal, promotion);
    }
    
    // 2. Áp dụng member tier discount
    BigDecimal tierDiscount = calculateTierDiscount(subtotal, user.getMemberTier());
    
    // 3. Tính giá cuối (không cộng dồn discount)
    BigDecimal maxDiscount = voucherDiscount.max(tierDiscount);
    BigDecimal finalPrice = subtotal.subtract(maxDiscount);
    
    return finalPrice.max(BigDecimal.ZERO); // Không âm
}
```

#### Dynamic Item Pricing
```java
// Giá động theo size và toppings
itemPrice = basePrice + sizeExtraPrice + sum(toppingPrices)
totalItemPrice = itemPrice × quantity

// Ví dụ: Trà sữa Oolong size L + Trân châu đen + Pudding
basePrice = 35,000đ
sizeL_extraPrice = 5,000đ
tranchau_price = 8,000đ
pudding_price = 10,000đ
quantity = 2

itemPrice = 35,000 + 5,000 + 8,000 + 10,000 = 58,000đ
totalItemPrice = 58,000 × 2 = 116,000đ
```

### 🛡️ Advanced Security Features

#### Risk Score Algorithm
```java
public int calculateRiskScore(User user) {
    int score = 0;
    
    // Failed login attempts (last 24h)
    score += getFailedLoginAttempts(user, 24) * 10;
    
    // Rate limit violations (last 7 days)
    score += getRateLimitViolations(user, 7) * 5;
    
    // Suspicious order patterns
    score += detectAbnormalOrderPatterns(user) * 20;
    
    // Multiple account detection
    if (hasMultipleAccountsFromSameIP(user)) {
        score += 25;
    }
    
    // Rapid order cancellations
    score += getOrderCancellationRate(user) * 15;
    
    return Math.min(score, 100); // Cap at 100
}
```

#### Auto Alert Generation
```java
// Tự động tạo cảnh báo dựa trên risk score
if (riskScore >= 70) {
    createAlert(user, AlertType.HIGH_RISK, "High risk score detected");
    
    if (riskScore >= 90) {
        blockUser(user, "Critical risk score - auto blocked");
        notifyAdmins(user, "User auto-blocked due to critical risk");
    }
}
```

### 👥 Group Order Advanced Logic

#### Invite Code Generation
```java
// Tạo mã mời 6 ký tự unique
public String generateInviteCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder code = new StringBuilder();
    
    do {
        code.setLength(0);
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
    } while (groupOrderRepository.existsByInviteCode(code.toString()));
    
    return code.toString();
}
```

#### Real-time Synchronization
```java
// WebSocket events cho group order
@EventListener
public void handleGroupOrderUpdate(GroupOrderUpdateEvent event) {
    // Gửi update cho tất cả members trong phiên
    List<User> members = groupOrderService.getMembers(event.getGroupOrderId());
    
    for (User member : members) {
        webSocketService.sendToUser(member.getId(), 
            "group-order-update", event.getData());
    }
}
```

### 🏆 Loyalty System Deep Dive

#### Spin Wheel Probability System
```java
public class SpinWheelService {
    private final Map<RewardType, Double> PROBABILITIES = Map.of(
        RewardType.POINTS_50, 0.30,      // 30%
        RewardType.POINTS_100, 0.25,     // 25%
        RewardType.VOUCHER_10K, 0.20,    // 20%
        RewardType.VOUCHER_20K, 0.15,    // 15%
        RewardType.FREE_TOPPING, 0.08,   // 8%
        RewardType.FREE_DRINK, 0.02      // 2%
    );
    
    public SpinResult spin(User user) {
        // Kiểm tra số lần spin theo tier
        validateSpinLimit(user);
        
        // Random với probability
        double random = Math.random();
        double cumulative = 0.0;
        
        for (Map.Entry<RewardType, Double> entry : PROBABILITIES.entrySet()) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                return createReward(user, entry.getKey());
            }
        }
        
        return createReward(user, RewardType.POINTS_50); // Fallback
    }
}
```

#### Auto Tier Upgrade
```java
@Scheduled(cron = "0 0 2 * * ?") // Chạy lúc 2h sáng hàng ngày
public void checkTierUpgrades() {
    List<User> users = userRepository.findUsersEligibleForUpgrade();
    
    for (User user : users) {
        MemberTier newTier = calculateNewTier(user.getPoints());
        
        if (newTier.ordinal() > user.getMemberTier().ordinal()) {
            user.setMemberTier(newTier);
            userRepository.save(user);
            
            // Gửi thông báo nâng cấp
            notificationService.sendTierUpgradeNotification(user, newTier);
            
            // Tặng bonus points
            loyaltyService.addBonusPoints(user, newTier.getBonusPoints());
        }
    }
}
```

### 💬 Live Chat Intelligence

#### Smart Queue Management
```java
public class ChatQueueService {
    
    public Manager assignOptimalManager(Conversation conversation) {
        List<Manager> availableManagers = getAvailableManagers();
        
        // Ưu tiên manager cùng store
        Manager storeManager = findManagerByStore(conversation.getUser().getPreferredStore());
        if (storeManager != null && storeManager.isAvailable()) {
            return storeManager;
        }
        
        // Chọn manager có ít conversation nhất
        return availableManagers.stream()
            .min(Comparator.comparing(Manager::getActiveConversationCount))
            .orElse(getDefaultManager());
    }
}
```

#### Auto Response System
```java
// Tự động trả lời các câu hỏi thường gặp
public class ChatbotService {
    
    public Optional<String> getAutoResponse(String message) {
        String normalizedMessage = message.toLowerCase().trim();
        
        if (containsKeywords(normalizedMessage, "giờ mở cửa", "mở cửa")) {
            return Optional.of("🕐 Cửa hàng mở cửa từ 7:00 - 22:00 hàng ngày");
        }
        
        if (containsKeywords(normalizedMessage, "địa chỉ", "ở đâu")) {
            return Optional.of("📍 Địa chỉ các cửa hàng:\n" +
                "- UTE Campus: Số 1 Võ Văn Ngân, Thủ Đức\n" +
                "- UTE Campus 2: Số 371 Nguyễn Kiệm, Gò Vấp");
        }
        
        if (containsKeywords(normalizedMessage, "thanh toán", "payment")) {
            return Optional.of("💳 Hỗ trợ thanh toán:\n" +
                "- Tiền mặt (COD)\n" +
                "- VNPay\n" +
                "- Chuyển khoản");
        }
        
        return Optional.empty();
    }
}
```

### 📊 Analytics & Predictive Features

#### Order Pattern Analysis
```java
public class PredictiveOrderService {
    
    public List<DrinkRecommendation> getPersonalizedRecommendations(User user) {
        // Phân tích lịch sử đặt hàng
        List<Order> orderHistory = orderRepository.findByUserOrderByCreatedAtDesc(user);
        
        // Tìm patterns
        Map<Long, Integer> drinkFrequency = calculateDrinkFrequency(orderHistory);
        Set<String> preferredToppings = extractPreferredToppings(orderHistory);
        String preferredSize = findMostOrderedSize(orderHistory);
        
        // Collaborative filtering
        List<User> similarUsers = findSimilarUsers(user);
        List<Drink> popularAmongSimilar = getPopularDrinksAmongUsers(similarUsers);
        
        // Combine và rank
        return rankRecommendations(drinkFrequency, popularAmongSimilar, 
                                 preferredToppings, preferredSize);
    }
    
    public OrderForecast predictDailyOrders(LocalDate date) {
        // Factors: day of week, weather, events, historical data
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        WeatherData weather = weatherService.getForecast(date);
        
        double baseMultiplier = getDayOfWeekMultiplier(dayOfWeek);
        double weatherMultiplier = getWeatherMultiplier(weather);
        double eventMultiplier = getEventMultiplier(date);
        
        int historicalAverage = getHistoricalAverage(dayOfWeek);
        
        int predictedOrders = (int) (historicalAverage * baseMultiplier * 
                                   weatherMultiplier * eventMultiplier);
        
        return new OrderForecast(date, predictedOrders, 
                               calculateConfidenceLevel(date));
    }
}
```

### 🔄 Real-time Features

#### WebSocket Event System
```java
@Component
public class RealTimeEventHandler {
    
    @EventListener
    public void handleOrderStatusChange(OrderStatusChangeEvent event) {
        Order order = event.getOrder();
        
        // Gửi cho user đặt hàng
        webSocketService.sendToUser(order.getUser().getId(), 
            "order-status-update", OrderDto.from(order));
        
        // Gửi cho managers
        List<Manager> managers = getManagersByStore(order.getStore());
        for (Manager manager : managers) {
            webSocketService.sendToUser(manager.getId(), 
                "new-order-update", OrderDto.from(order));
        }
    }
    
    @EventListener
    public void handleNewMessage(ChatMessageEvent event) {
        Message message = event.getMessage();
        Conversation conversation = message.getConversation();
        
        // Gửi cho tất cả participants
        webSocketService.sendToConversation(conversation.getId(), 
            "new-message", MessageDto.from(message));
        
        // Push notification nếu offline
        if (!isUserOnline(conversation.getUser())) {
            pushNotificationService.sendChatNotification(
                conversation.getUser(), message.getContent());
        }
    }
}
```

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

## 🏗️ Kiến trúc & Công nghệ

### 🎯 Kiến trúc Tổng quan

Backend được xây dựng theo mô hình **Spring Boot MVC** với kiến trúc phân lớp:

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
│      Security/Filter Layer              │  ← JWT, Rate Limit, IP Block
│  (JwtFilter, RateLimitFilter, ...)     │
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

### � rCác Layer Chi tiết

#### 🎮 Controller Layer
- **AuthController**: Xác thực, OTP, JWT
- **DrinkController**: Quản lý menu, tìm kiếm
- **OrderController**: Tạo đơn, theo dõi trạng thái
- **CartController**: Giỏ hàng cá nhân
- **GroupOrderController**: Đặt hàng nhóm
- **LiveChatController**: Chat real-time
- **LoyaltyController**: Điểm thưởng, vòng xoay
- **UserMonitoringController**: Giám sát admin

#### ⚙️ Service Layer
- **AuthService**: Đăng ký OTP, JWT, bảo mật
- **OrderService**: Business logic đơn hàng
- **UserMonitoringService**: Risk score, alerts
- **LoyaltyService**: Tích điểm, member tiers
- **GroupOrderService**: Collaborative ordering
- **LiveChatService**: WebSocket messaging

#### 🛡️ Security & Filter Layer
- **JwtAuthenticationFilter**: JWT validation
- **RateLimitFilter**: Chống spam/abuse
- **BlockedIPFilter**: IP blacklist
- **SecurityConfig**: Role-based authorization

#### 🗄️ Repository & Model Layer
- **JPA Repositories**: Optimized queries
- **Entity Models**: User, Order, Drink, Promotion
- **Custom Queries**: Performance optimization
- **Soft Delete**: Data retention

### 🚀 Tính năng Độc đáo

#### 🛡️ User Monitoring System
```java
// Risk Score Calculation
riskScore = failedLoginAttempts * 10 
          + suspiciousActivities * 15
          + rateLimit violations * 5
          + abnormalOrderPatterns * 20

// Auto Alert Generation
if (riskScore > THRESHOLD) {
    createAlert(user, riskLevel, activities);
    if (riskScore > CRITICAL_THRESHOLD) {
        blockUser(user);
    }
}
```

#### 👥 Group Order System
- **Collaborative Ordering**: Mã mời 6 ký tự
- **Real-time Updates**: WebSocket notifications
- **Role Management**: Host vs Members
- **Chat Integration**: In-session messaging

#### 🏆 Loyalty & Gamification
- **Member Tiers**: Bronze → Silver → Gold → Diamond
- **Spin Wheel**: Probability-based rewards
- **Points System**: Earn on orders, redeem vouchers
- **Tier Benefits**: Progressive discounts

#### 💬 Live Chat System
- **WebSocket**: Real-time messaging
- **Queue Management**: FIFO for managers
- **Conversation History**: Persistent storage
- **Auto-assignment**: By store location

### ⚡ Performance Optimization

#### 🔍 N+1 Query Prevention
```java
// Before: N+1 Problem
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems(); // N additional queries
}

// After: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.user.id = :userId")
List<Order> findOrdersWithItems(@Param("userId") Long userId);
```

#### 📊 Batch Loading
```java
// Batch load sizes and toppings
@BatchSize(size = 10)
@OneToMany(mappedBy = "drink", fetch = FetchType.LAZY)
private List<DrinkSize> sizes;
```

#### 🗄️ Database Indexing
```sql
-- Optimized indexes
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_drinks_category_active ON drinks(category_id, is_active);
CREATE INDEX idx_promotions_code_active ON promotions(code, is_active);
```

### 🔒 Security Features

#### 🔑 Multi-layer Authentication
```java
// JWT + Role-based + Method-level security
@PreAuthorize("hasRole('MANAGER') and @orderService.canAccessOrder(#orderId, authentication.name)")
public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long orderId) {
    // Implementation
}
```

#### 🚫 Rate Limiting
```java
// Different limits per endpoint type
AUTH_ENDPOINTS: 5 requests/minute
OTP_ENDPOINTS: 3 requests/minute  
GENERAL_ENDPOINTS: 100 requests/minute
```

#### 🛡️ Input Validation & Sanitization
```java
// Comprehensive validation
@Valid @RequestBody CreateOrderRequest request
// + Custom validators for business rules
// + SQL injection prevention
// + XSS protection
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

### Core Technologies

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **Java** | 17 | Programming language |
| **Spring Boot** | 3.5.7 | Application framework |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Spring Data JPA** | 3.x | Database ORM |
| **MySQL** | 8.0 | Relational database |
| **JWT** | 0.11.5 | Token-based authentication |

### Advanced Features

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **WebSocket** | Spring 6.x | Real-time communication |
| **Redis** | 7.x | Caching & session storage |
| **Swagger/OpenAPI** | 3.0 | API documentation |
| **Lombok** | Latest | Reduce boilerplate code |
| **Maven** | 3.6+ | Build & dependency management |

### External Integrations

| Service | Mục đích | Status |
|---------|----------|--------|
| **VNPay** | Payment gateway | ✅ Active |
| **SendGrid** | Email service | ✅ Active |
| **OneSignal** | Push notifications | ✅ Active |
| **Cloudinary** | Image storage | ✅ Active |
| **Weather API** | Weather data | ✅ Active |

### Security & Monitoring

| Feature | Implementation | Status |
|---------|----------------|--------|
| **Rate Limiting** | Custom filter + Redis | ✅ Active |
| **IP Blocking** | Database + filter | ✅ Active |
| **Risk Scoring** | ML-based algorithm | ✅ Active |
| **Activity Logging** | AOP + database | ✅ Active |
| **Alert System** | Event-driven | ✅ Active |

### Performance Optimizations

| Technique | Implementation | Impact |
|-----------|----------------|--------|
| **N+1 Prevention** | JOIN FETCH queries | 🚀 High |
| **Batch Loading** | @BatchSize annotation | 🚀 High |
| **Database Indexing** | Strategic indexes | 🚀 High |
| **Caching** | Redis caching | 🚀 Medium |
| **Connection Pooling** | HikariCP | 🚀 Medium |

---

## 📈 Monitoring & Analytics

### 📊 Application Metrics

- **Health Checks**: Endpoint monitoring
- **Performance Metrics**: Response time, throughput
- **Error Tracking**: Exception logging và analysis
- **Database Metrics**: Query performance, connection pool

### 🔍 Business Analytics

- **Order Analytics**: Revenue, popular items, peak hours
- **User Behavior**: Activity patterns, retention rates
- **Promotion Effectiveness**: Usage rates, ROI analysis
- **Predictive Analytics**: Order forecasting, demand planning

### 🛡️ Security Monitoring

- **Failed Login Tracking**: Brute force detection
- **Suspicious Activity Detection**: Anomaly detection
- **IP Reputation**: Blacklist management
- **Risk Assessment**: User risk scoring

---

## 🚀 Deployment & Scaling

### 🐳 Containerization

```dockerfile
# Multi-stage Docker build
FROM openjdk:17-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ☁️ Cloud Deployment

- **Railway**: Production deployment
- **Render**: Backup deployment
- **Aiven MySQL**: Cloud database
- **Cloudinary**: CDN for images

### 📊 Load Balancing & Scaling

```yaml
# docker-compose.yml for horizontal scaling
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080-8082:8080"
    deploy:
      replicas: 3
    environment:
      - SPRING_PROFILES_ACTIVE=production
  
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    depends_on:
      - app
```

---

## 🔧 Configuration Management

### 🌍 Environment Profiles

```properties
# application-dev.properties
spring.datasource.url=jdbc:mysql://localhost:3306/utetea_dev
logging.level.com.utetea=DEBUG
rate.limit.enabled=false

# application-prod.properties  
spring.datasource.url=${DATABASE_URL}
logging.level.com.utetea=INFO
rate.limit.enabled=true
security.jwt.secret=${JWT_SECRET}
```

### 🔐 Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/drinks/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter(), 
                JwtAuthenticationFilter.class)
            .addFilterBefore(blockedIPFilter(), 
                RateLimitFilter.class)
            .build();
    }
}
```

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
