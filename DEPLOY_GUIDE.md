# 🚀 Hướng dẫn Deploy Backend lên Render.com

## Bước 1: Push code lên GitHub

```bash
cd Backend_APP
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/utetea-backend.git
git push -u origin main
```

## Bước 2: Tạo tài khoản Render.com

1. Truy cập https://render.com
2. Đăng ký bằng GitHub
3. Xác nhận email

## Bước 3: Tạo Web Service

1. Click **"New +"** → **"Web Service"**
2. Chọn **"Build and deploy from a Git repository"**
3. Connect GitHub repo của bạn
4. Cấu hình:
   - **Name:** `utetea-backend`
   - **Region:** Singapore (gần VN nhất)
   - **Branch:** `main`
   - **Runtime:** `Docker`
   - **Plan:** `Free`

5. Click **"Create Web Service"**

## Bước 4: Chờ deploy

- Render sẽ tự động build và deploy
- Mất khoảng 5-10 phút lần đầu
- Sau khi xong, bạn sẽ có URL như: `https://utetea-backend.onrender.com`

## Bước 5: Cập nhật URL trong app Android

Sau khi có URL từ Render, cập nhật file `RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "https://utetea-backend.onrender.com/api/"
```

---

## ⚠️ Lưu ý quan trọng

### 1. Free tier sẽ sleep sau 15 phút
- Request đầu tiên sau khi sleep sẽ mất ~30s để wake up
- Giải pháp: Dùng UptimeRobot (miễn phí) để ping mỗi 14 phút

### 2. Database đã có sẵn trên Aiven Cloud
- Không cần thay đổi gì, database đã được config trong application.properties

### 3. VNPay Return URL
- Cần cập nhật `vnpay.return-url` trong application.properties:
```properties
vnpay.return-url=https://utetea-backend.onrender.com/api/vnpay/callback
```

---

## 🔧 Cách giữ server không sleep (UptimeRobot)

1. Truy cập https://uptimerobot.com
2. Đăng ký miễn phí
3. Add New Monitor:
   - **Monitor Type:** HTTP(s)
   - **Friendly Name:** UTE Tea Backend
   - **URL:** `https://utetea-backend.onrender.com/api/categories`
   - **Monitoring Interval:** 5 minutes
4. Create Monitor

---

## 📱 Cập nhật App Android

Sau khi deploy xong, sửa file `RetrofitClient.kt`:

```kotlin
object RetrofitClient {
    // URL production trên Render
    private const val BASE_URL = "https://utetea-backend.onrender.com/api/"
    
    // Hoặc để linh hoạt hơn:
    // private const val BASE_URL = BuildConfig.API_BASE_URL
}
```

---

## ✅ Kiểm tra deploy thành công

Truy cập các URL sau để test:

1. **Health check:** `https://your-app.onrender.com/api/categories`
2. **Swagger UI:** `https://your-app.onrender.com/swagger-ui.html`
3. **API Docs:** `https://your-app.onrender.com/v3/api-docs`

---

## 🆘 Troubleshooting

### Lỗi build failed
- Kiểm tra logs trong Render dashboard
- Đảm bảo pom.xml đúng

### Lỗi database connection
- Database Aiven Cloud đã public, không cần thay đổi

### App Android không kết nối được
- Kiểm tra URL đã đúng chưa
- Kiểm tra HTTPS (không phải HTTP)
- Thêm `android:usesCleartextTraffic="true"` trong AndroidManifest.xml nếu cần
