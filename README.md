# 🍵 UTE TEA - Backend API

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)

**Backend API thông minh cho ứng dụng đặt trà sữa UTE Tea**

[Tính năng](#-tính-năng) • [Cài đặt](#️-cài-đặt) • [API](#-api-endpoints) • [Tech Stack](#-tech-stack)

</div>

---

## 📖 Giới thiệu

Backend API được xây dựng bằng **Spring Boot 3.5.7** với **MySQL 8.0**, phục vụ ứng dụng đặt trà sữa trực tuyến cho sinh viên UTE.

### 🎯 Điểm nổi bật

- 🤖 **AI Predictive Order** - Gợi ý món dựa trên lịch sử và thời tiết
- 👥 **Group Order** - Đặt hàng nhóm real-time với WebSocket
- 🎡 **Loyalty System** - 4-tier membership + spin wheel
- �️ **ảUser Monitoring** - Risk scoring và automated alerts
- � ***Live Chat** - Hỗ trợ khách hàng với AI chatbot
- � **Secureity** - JWT + Rate limiting + IP blocking

---

## ✨ Tính năng

### 👤 User Features
- **Authentication**: Đăng ký OTP, JWT login, biometric support
- **Shopping**: Menu browsing, cart management, order tracking
- **AI Suggestions**: Smart recommendations dựa trên behavior
- **Group Order**: Collaborative ordering với invite codes
- **Loyalty**: Points system, spin wheel, tier benefits
- **Live Chat**: Real-time support với managers

### 👨‍💼 Manager Features
- **Dashboard**: Revenue analytics, order management
- **Products**: CRUD drinks, categories, promotions
- **Orders**: Status updates, real-time tracking
- **Analytics**: Sales forecasting, peak hours analysis
- **Chat Management**: Handle customer conversations

### 🛡️ Admin Features
- **User Monitoring**: Activity tracking, risk assessment
- **Security**: IP blocking, rate limiting, alerts
- **System Management**: User roles, system configuration

---

## ⚙️ Cài đặt

### Yêu cầu
- Java JDK 17+
- Maven 3.6+
- MySQL 8.0+ (optional - cloud DB available)

### Chạy ứng dụng

```bash
# Clone repository
git clone <repository-url>
cd Backend_APP

# Chạy với Maven Wrapper
./mvnw spring-boot:run        # Mac/Linux
.\mvnw.cmd spring-boot:run    # Windows

# Hoặc build JAR
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Kiểm tra
- Server: http://localhost:8080
- Health check: `GET /api/auth/health`
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## 📡 API Endpoints

### 🔓 Public APIs
```
POST /api/auth/register-with-otp    # Đăng ký với OTP
POST /api/auth/login                # Đăng nhập
GET  /api/drinks                    # Danh sách món
GET  /api/promotions                # Voucher active
```

### 🔐 User APIs (JWT required)
```
# Profile
GET  /api/me                        # Thông tin profile
PUT  /api/me                        # Cập nhật profile
POST /api/me/avatar                 # Upload avatar

# Cart & Orders
POST /api/cart/add                  # Thêm vào giỏ
GET  /api/cart                      # Xem giỏ hàng
POST /api/orders                    # Tạo đơn hàng
GET  /api/orders/my                 # Đơn hàng của tôi

# Group Order
POST /api/group-orders              # Tạo phiên nhóm
POST /api/group-orders/join         # Tham gia nhóm
POST /api/group-orders/{id}/checkout # Thanh toán nhóm

# Loyalty
GET  /api/loyalty/points            # Điểm thưởng
POST /api/loyalty/spin              # Quay vòng may mắn
```

### 👨‍💼 Manager APIs
```
GET  /api/manager/summary           # Dashboard tổng quan
GET  /api/manager/forecast          # Dự báo doanh thu
PUT  /api/orders/{id}/status        # Cập nhật trạng thái đơn
POST /api/promotions/manager        # Tạo voucher
```

### 🛡️ Admin APIs
```
GET  /api/monitoring/dashboard      # Monitoring dashboard
GET  /api/monitoring/alerts         # System alerts
POST /api/admin/drinks              # Quản lý sản phẩm
```

---

## 🏗️ Tech Stack

### Core Technologies
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Programming language |
| **Spring Boot** | 3.5.7 | Application framework |
| **Spring Security** | 6.x | Authentication & Authorization |
| **MySQL** | 8.0 | Database |
| **JWT** | 0.11.5 | Token authentication |
| **WebSocket** | Spring 6.x | Real-time communication |

### External Services
| Service | Purpose |
|---------|---------|
| **OneSignal** | Push notifications |
| **SendGrid** | Email service |
| **VNPay** | Payment gateway |
| **Weather API** | Weather data |
| **Aiven MySQL** | Cloud database |

---

## 🔒 Security Features

- **JWT Authentication** với auto-refresh
- **Rate Limiting** chống spam/abuse
- **IP Blocking** với whitelist management
- **User Monitoring** với risk scoring
- **Input Validation** chống injection
- **CORS Configuration** cho mobile apps

---

## 📊 Database Schema

### Core Tables
```sql
users (id, username, password, role, memberTier, points, riskScore)
orders (id, userId, storeId, status, totalPrice, finalPrice)
order_items (id, orderId, drinkId, quantity, unitPrice)
drinks (id, name, basePrice, categoryId, isActive)
promotions (id, code, type, value, startDate, endDate)
group_orders (id, hostUserId, inviteCode, status)
```

### Monitoring Tables
```sql
user_activities (id, userId, activityType, ipAddress, riskLevel)
monitoring_alerts (id, userId, alertType, severity, isHandled)
loyalty_rewards (id, userId, voucherCode, isUsed)
```

---

## 🚀 Deployment

### Docker
```dockerfile
FROM openjdk:17-jre-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Variables
```properties
# Database
DATABASE_URL=jdbc:mysql://host:port/database
DB_USERNAME=username
DB_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# External Services
ONESIGNAL_APP_ID=your-app-id
SENDGRID_API_KEY=your-api-key
VNPAY_TMN_CODE=your-tmn-code
```

---

## 🔧 Configuration

### Application Properties
```properties
# Server
server.port=8080
server.servlet.context-path=/

# Database
spring.datasource.url=${DATABASE_URL}
spring.jpa.hibernate.ddl-auto=update

# Security
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}

# Rate Limiting
rate.limit.requests-per-minute=100
rate.limit.enabled=true
```

---

## 📈 Monitoring & Analytics

### Health Checks
- Application health: `/actuator/health`
- Database connectivity
- External service status

### Metrics
- API response times
- Database query performance
- User activity patterns
- Error rates và exceptions

---

## 🐛 Troubleshooting

### Common Issues

**Port 8080 in use:**
```properties
server.port=8081
```

**Database connection failed:**
- Check DATABASE_URL format
- Verify credentials
- Ensure MySQL is running

**JWT token expired:**
- Check JWT_EXPIRATION setting
- Verify JWT_SECRET configuration

---

## 📄 License

MIT License - Đồ án Lập trình Di động UTE

---

## 👥 Team

**UTE Tea Development Team**
- Backend: Spring Boot + MySQL
- Frontend: Android Kotlin
- Database: MySQL 8.0 on Aiven Cloud

---

<div align="center">

**🍵 Made with ❤️ for UTE Students 🍵**

</div>