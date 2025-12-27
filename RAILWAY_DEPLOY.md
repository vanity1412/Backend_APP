# 🚂 Hướng dẫn Deploy Backend lên Railway.app

## ✨ Ưu điểm Railway so với Render
- ✅ **Không sleep** - Server chạy 24/7
- ✅ **Miễn phí $5/tháng** - Đủ cho app nhỏ
- ✅ **Deploy nhanh** - Chỉ 2-3 phút
- ✅ **Giao diện đẹp** - Dễ dùng

---

## 📝 Bước 1: Push code lên GitHub

```bash
cd Backend_APP
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/utetea-backend.git
git push -u origin main
```

---

## 📝 Bước 2: Tạo tài khoản Railway

1. Truy cập **https://railway.app**
2. Click **"Login"** → **"Login with GitHub"**
3. Authorize Railway

---

## 📝 Bước 3: Tạo Project mới

1. Click **"New Project"**
2. Chọn **"Deploy from GitHub repo"**
3. Chọn repo `utetea-backend`
4. Railway sẽ tự động detect Dockerfile và bắt đầu build

---

## 📝 Bước 4: Cấu hình Environment Variables (Tùy chọn)

Nếu muốn thay đổi config, vào **Variables** tab và thêm:

```
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

**Lưu ý:** Database đã config sẵn trong application.properties, không cần thêm.

---

## 📝 Bước 5: Generate Domain

1. Vào project → **Settings** tab
2. Scroll xuống **Networking**
3. Click **"Generate Domain"**
4. Bạn sẽ có URL như: `https://utetea-backend-production.up.railway.app`

---

## 📝 Bước 6: Cập nhật App Android

Sửa file `RetrofitClient.kt`:

```kotlin
companion object {
    // ✅ URL Railway Production
    private const val BASE_URL = "https://utetea-backend-production.up.railway.app/api/"
}
```

---

## ⏱️ Thời gian deploy

- **Lần đầu:** 3-5 phút (build Docker image)
- **Các lần sau:** 1-2 phút (có cache)

---

## 💰 Chi phí

Railway cho **$5 miễn phí mỗi tháng**:
- ~500 giờ chạy (đủ chạy 24/7 cả tháng)
- 512MB RAM
- Không giới hạn bandwidth

**Ước tính:** App của bạn sẽ dùng khoảng $3-4/tháng → **Miễn phí!**

---

## ✅ Kiểm tra deploy thành công

Truy cập các URL:

1. **Categories:** `https://your-app.up.railway.app/api/categories`
2. **Swagger:** `https://your-app.up.railway.app/swagger-ui.html`
3. **Chatbot health:** `https://your-app.up.railway.app/api/chatbot/health`

---

## 🔧 Cập nhật VNPay Return URL

Sau khi có URL Railway, sửa `application.properties`:

```properties
vnpay.return-url=https://your-app.up.railway.app/api/vnpay/callback
```

Rồi push lại lên GitHub, Railway sẽ tự động redeploy.

---

## 📱 Build APK cho điện thoại

1. Sửa URL trong `RetrofitClient.kt`
2. Android Studio → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
3. File APK: `app/build/outputs/apk/debug/app-debug.apk`
4. Copy sang điện thoại và cài đặt

---

## 🆘 Troubleshooting

### Build failed
- Kiểm tra **Deployments** tab để xem logs
- Đảm bảo Dockerfile đúng

### App không kết nối được
- Kiểm tra URL có đúng không (HTTPS, không có port)
- Kiểm tra domain đã generate chưa

### Hết $5 credit
- Upgrade lên Hobby plan ($5/tháng)
- Hoặc tạo account mới 😅

---

## 🎯 So sánh Railway vs Render

| Tính năng | Railway | Render |
|-----------|---------|--------|
| Sleep | ❌ Không | ✅ Có (15 phút) |
| Miễn phí | $5/tháng | Unlimited |
| Tốc độ deploy | Nhanh | Trung bình |
| Giao diện | Đẹp | Bình thường |
| **Khuyên dùng** | ✅ Cho production | Cho test |

---

## 🚀 Quick Start (TL;DR)

```bash
# 1. Push code
cd Backend_APP
git add . && git commit -m "deploy" && git push

# 2. Vào railway.app → New Project → Deploy from GitHub

# 3. Generate Domain

# 4. Sửa RetrofitClient.kt với URL mới

# 5. Build APK và cài lên điện thoại
```

**Done! 🎉**
