# 🚀 QUICK START - UTE TEA BACKEND

## ⚡ Chạy Backend trong 3 bước

### 1. Chạy Backend
```bash
.\mvnw.cmd spring-boot:run
```

### 2. Mở Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 3. Test API

#### Login để lấy JWT token:
```bash
POST /api/auth/login
{
  "usernameOrPhone": "ute_student_01",
  "password": "123456"
}
```

#### Copy token và click "Authorize" ở Swagger UI:
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🧪 Chạy Tests

```bash
# Tất cả tests
.\mvnw.cmd test

# Chỉ unit tests
.\mvnw.cmd test -Dtest=OrderServiceTest

# Chỉ integration tests
.\mvnw.cmd test -Dtest=OrderIntegrationTest
```

---

## 📱 Connect từ Android

### 1. Tìm IP máy tính:
```bash
ipconfig
# Ví dụ: 192.168.1.100
```

### 2. Base URL trong Android:
```java
public static final String BASE_URL = "http://192.168.1.100:8080";
```

### 3. Test trên điện thoại:
```
http://192.168.1.100:8080/swagger-ui.html
```

---

## 📚 Tài liệu đầy đủ

- **API Documentation**: `API-DOCUMENTATION.md`
- **Setup Guide**: `HUONG-DAN-CHAY-API.md`
- **Testing Guide**: `TESTING-GUIDE.md`
- **Completion Summary**: `BACKEND-COMPLETION-SUMMARY.md`

---

## ✅ Checklist

- [x] Backend chạy được
- [x] Swagger UI hiển thị
- [x] Login thành công
- [x] Tests pass
- [x] Connect từ Android

---

**Ready to go!** 🎉
