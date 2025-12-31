# Hướng dẫn cấu hình Backend

## Bước 1: Tạo file cấu hình

Copy file `application.properties.example` thành `application.properties`:

```bash
cd src/main/resources
cp application.properties.example application.properties
```

## Bước 2: Điền thông tin

Mở file `application.properties` và thay thế các giá trị sau:

### Database
- `YOUR_DB_HOST`: Host database của bạn
- `YOUR_DB_USERNAME`: Username database
- `YOUR_DB_PASSWORD`: Password database

### Email (Gmail)
- `your-email@gmail.com`: Email Gmail của bạn
- `your-app-password`: App password từ Google (không phải password thường)
  - Tạo tại: https://myaccount.google.com/apppasswords

### JWT
- `YOUR_JWT_SECRET_KEY_MINIMUM_256_BITS`: Secret key tối thiểu 256 bits

### Github Token
- `YOUR_GITHUB_TOKEN`: Personal access token từ Github

### VNPAY
- `YOUR_VNPAY_TMN_CODE`: Mã TMN từ VNPAY
- `YOUR_VNPAY_HASH_SECRET`: Hash secret từ VNPAY
- `https://your-domain.com`: Domain của bạn

### HttpSMS
- `YOUR_HTTPSMS_API_KEY`: API key từ HttpSMS
- `YOUR_PHONE_NUMBER`: Số điện thoại của bạn

### Weather API
- `YOUR_WEATHER_API_KEY`: API key từ OpenWeatherMap
  - Đăng ký miễn phí tại: https://openweathermap.org/api

## Bước 3: Deploy lên Railway

Khi deploy lên Railway, set các biến môi trường sau trong Railway Dashboard:

```
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
SENDGRID_API_KEY=your-sendgrid-key (optional)
WEATHER_API_KEY=your-weather-key
```

## Lưu ý bảo mật

- **KHÔNG BAO GIỜ** commit file `application.properties` lên Git
- File này đã được thêm vào `.gitignore`
- Chỉ commit file `application.properties.example`
- Mỗi developer cần tạo file `application.properties` riêng trên máy local
