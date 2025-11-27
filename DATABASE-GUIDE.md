# 💾 HƯỚNG DẪN DATABASE - UTE TEA BACKEND

## 📋 Mục lục
1. [Tổng quan Database](#tổng-quan-database)
2. [Cấu trúc Database](#cấu-trúc-database)
3. [Cách setup Database](#cách-setup-database)
4. [Import dữ liệu mẫu](#import-dữ-liệu-mẫu)
5. [Quản lý Database](#quản-lý-database)
6. [Queries thường dùng](#queries-thường-dùng)
7. [Backup & Restore](#backup--restore)

---

## 🎯 Tổng quan Database

### Thông tin Database:
- **Tên Database:** `LTDD_Thongtesst` (hoặc `LTDD_Thong` trên cloud)
- **Engine:** MySQL 8.0+
- **Charset:** utf8mb4
- **Collation:** utf8mb4_unicode_ci
- **Timezone:** UTC

### Database Cloud (Đã cấu hình sẵn):
```
Host: mysql-16b47c6b-phongtran080809-7c70.c.aivencloud.com
Port: 26260
Database: LTDD_Thong
Username: avnadmin
Password: AVNS_Ix83Fzpvp1FUIgDMvry
SSL: Required
```

---

## 📊 Cấu trúc Database

### Sơ đồ quan hệ các bảng:

```
users (Người dùng)
  ↓
orders (Đơn hàng) ← promotions (Mã giảm giá)
  ↓              ← stores (Cửa hàng)
order_items (Chi tiết đơn)
  ↓              ← drinks (Món nước) ← drink_categories (Loại)
  ↓              ← drink_sizes (Size)
order_item_toppings ← drink_toppings (Topping)
```


### Chi tiết các bảng:

#### 1. **users** - Người dùng
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    phone VARCHAR(15) UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    address VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    member_tier VARCHAR(20) DEFAULT 'BRONZE',
    points INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    otp VARCHAR(255),
    otp_expiry DATETIME,
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Roles:**
- `USER`: Khách hàng thông thường
- `MANAGER`: Quản lý cửa hàng

**Member Tiers:**
- `BRONZE`: Thành viên đồng (mặc định)
- `SILVER`: Thành viên bạc
- `GOLD`: Thành viên vàng

---

#### 2. **drink_categories** - Loại đồ uống
```sql
CREATE TABLE drink_categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    display_order INT DEFAULT 0,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Categories có sẵn:**
1. Milk Tea - Trà sữa Houjicha
2. Fruit Tea - Trà trái cây
3. Macchiato - Trà kem cheese
4. Special - Đồ uống đặc biệt

---

#### 3. **drinks** - Món nước
```sql
CREATE TABLE drinks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    base_price DECIMAL(10,2) NOT NULL,
    category_id INT UNSIGNED,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES drink_categories(id)
);
```

**Tổng cộng:** 16 món nước
- Milk Tea: 4 món
- Fruit Tea: 5 món
- Macchiato: 3 món
- Special: 4 món

---

#### 4. **drink_sizes** - Size món nước
```sql
CREATE TABLE drink_sizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drink_id BIGINT NOT NULL,
    size_name VARCHAR(50) NOT NULL,
    extra_price DECIMAL(10,2) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);
```

**Sizes:**
- `M`: Size vừa (giá gốc)
- `L`: Size lớn (+4,000đ - 6,000đ)
- `Jumbo`: Size đại (+10,000đ) - chỉ một số món

---

#### 5. **drink_toppings** - Topping
```sql
CREATE TABLE drink_toppings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drink_id BIGINT,
    topping_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);
```

**Toppings chung (drink_id = NULL):**
- Trân châu đen: 7,000đ
- Trân châu trắng: 8,000đ
- Thạch phô mai: 9,000đ
- Thạch củ năng: 8,000đ
- Kem cheese: 10,000đ
- Pudding trứng: 9,000đ

**Toppings riêng:** Một số món có topping đặc biệt

---

#### 6. **stores** - Cửa hàng
```sql
CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    open_time TIME,
    close_time TIME,
    phone VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Stores có sẵn:**
1. UTE Tea - Cơ sở 1: Số 1 Võ Văn Ngân
2. UTE Tea - Cơ sở 2: Khu KTX UTE

---

#### 7. **promotions** - Mã giảm giá
```sql
CREATE TABLE promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    min_order_value DECIMAL(10,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Discount Types:**
- `PERCENT`: Giảm theo phần trăm
- `FIXED`: Giảm số tiền cố định

**Promotions có sẵn:**
- STUDENT20: Giảm 20% (min 50k)
- FREESHIPUTE: Giảm 15k (min 60k)
- COMBO4UTE: Giảm 30k (min 120k)

---

#### 8. **orders** - Đơn hàng
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    pickup_time DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) DEFAULT 0,
    final_price DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    promotion_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(id)
);
```

**Order Types:**
- `DELIVERY`: Giao hàng
- `PICKUP`: Tự đến lấy

**Order Status:**
- `PENDING`: Chờ xác nhận
- `MAKING`: Đang pha chế
- `SHIPPING`: Đang giao hàng
- `DONE`: Hoàn thành
- `CANCELED`: Đã hủy

**Payment Methods:**
- `COD`: Tiền mặt
- `VNPAY`: VNPay
- `MOMO`: MoMo

---

#### 9. **order_items** - Chi tiết đơn hàng
```sql
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    drink_id BIGINT NOT NULL,
    drink_name_snapshot VARCHAR(100),
    size_name_snapshot VARCHAR(50),
    quantity INT NOT NULL,
    item_price DECIMAL(10,2) NOT NULL,
    note VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);
```

**Snapshot fields:** Lưu tên món và size tại thời điểm đặt hàng (phòng trường hợp thay đổi menu)

---

#### 10. **order_item_toppings** - Topping trong đơn
```sql
CREATE TABLE order_item_toppings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    topping_name_snapshot VARCHAR(100),
    topping_price_snapshot DECIMAL(10,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);
```

---

## 🔧 Cách setup Database

### Option 1: Sử dụng Database Cloud (KHUYẾN NGHỊ)

Database cloud đã được setup sẵn với đầy đủ dữ liệu. Bạn chỉ cần:

1. Kiểm tra file `application.properties` đã đúng:
```properties
spring.datasource.url=jdbc:mysql://mysql-16b47c6b-phongtran080809-7c70.c.aivencloud.com:26260/LTDD_Thong?sslMode=REQUIRED
spring.datasource.username=avnadmin
spring.datasource.password=AVNS_Ix83Fzpvp1FUIgDMvry
```

2. Chạy application:
```bash
.\mvnw.cmd spring-boot:run
```

✅ **XONG!** Database đã sẵn sàng.

---

### Option 2: Setup MySQL Local

#### Bước 1: Cài đặt MySQL 8.0+

**Windows:**
- Download MySQL Installer từ: https://dev.mysql.com/downloads/installer/
- Chọn "MySQL Server 8.0.x"
- Cài đặt và nhớ password root

**Mac:**
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

---

#### Bước 2: Tạo Database

Mở MySQL Workbench hoặc command line:

```sql
-- Tạo database
CREATE DATABASE IF NOT EXISTS LTDD_Thongtesst 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Sử dụng database
USE LTDD_Thongtesst;

-- Kiểm tra
SHOW DATABASES;
```

---

#### Bước 3: Tạo bảng drink_categories

Chạy file: `src/main/resources/schema-categories.sql`

```sql
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

-- Thêm cột category_id vào bảng drinks (nếu chưa có)
ALTER TABLE drinks 
ADD COLUMN category_id INT UNSIGNED NULL AFTER base_price,
ADD CONSTRAINT fk_drinks_category 
    FOREIGN KEY (category_id) REFERENCES drink_categories(id),
ADD INDEX idx_drinks_category_id (category_id);
```

---

#### Bước 4: Để Hibernate tạo các bảng còn lại

Cấu hình trong `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Khi chạy application lần đầu, Hibernate sẽ tự động tạo các bảng:
- users
- drinks
- drink_sizes
- drink_toppings
- stores
- promotions
- orders
- order_items
- order_item_toppings

---

## 📥 Import dữ liệu mẫu

### Cách 1: Dùng MySQL Workbench (Đơn giản nhất)

1. Mở MySQL Workbench
2. Kết nối đến database `LTDD_Thongtesst`
3. File → Open SQL Script
4. Chọn file: `src/main/resources/data-ltdd.sql`
5. Click Execute (⚡ icon)
6. Kiểm tra kết quả

---

### Cách 2: Dùng Command Line

```bash
# Windows
mysql -u root -p LTDD_Thongtesst < src/main/resources/data-ltdd.sql

# Mac/Linux
mysql -u root -p LTDD_Thongtesst < src/main/resources/data-ltdd.sql
```

Nhập password MySQL khi được yêu cầu.

---

### Cách 3: Copy-Paste SQL

1. Mở file `src/main/resources/data-ltdd.sql`
2. Copy toàn bộ nội dung
3. Paste vào MySQL Workbench Query tab
4. Execute

---

### Kiểm tra dữ liệu đã import

```sql
-- Kiểm tra categories
SELECT * FROM drink_categories;
-- Kết quả: 4 categories

-- Kiểm tra drinks
SELECT COUNT(*) FROM drinks;
-- Kết quả: 16 drinks

-- Kiểm tra users
SELECT username, role FROM users;
-- Kết quả: 4 users (1 MANAGER, 3 USER)

-- Kiểm tra stores
SELECT * FROM stores;
-- Kết quả: 2 stores

-- Kiểm tra promotions
SELECT code, discount_value FROM promotions;
-- Kết quả: 3 promotions
```

---

## 🔍 Queries thường dùng

### Xem tất cả món nước theo category

```sql
SELECT 
    c.name AS category_name,
    d.name AS drink_name,
    d.base_price,
    d.is_active
FROM drinks d
JOIN drink_categories c ON d.category_id = c.id
WHERE d.is_active = 1
ORDER BY c.display_order, d.name;
```

---

### Xem sizes của một món

```sql
SELECT 
    d.name AS drink_name,
    ds.size_name,
    d.base_price,
    ds.extra_price,
    (d.base_price + ds.extra_price) AS final_price
FROM drinks d
JOIN drink_sizes ds ON d.id = ds.drink_id
WHERE d.id = 1;
```

---

### Xem toppings có sẵn

```sql
SELECT 
    topping_name,
    price,
    CASE 
        WHEN drink_id IS NULL THEN 'Chung'
        ELSE 'Riêng'
    END AS type
FROM drink_toppings
WHERE is_active = 1
ORDER BY type, topping_name;
```

---

### Xem đơn hàng của user

```sql
SELECT 
    o.id AS order_id,
    u.username,
    s.store_name,
    o.type,
    o.status,
    o.total_price,
    o.discount,
    o.final_price,
    o.created_at
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN stores s ON o.store_id = s.id
WHERE u.id = 2
ORDER BY o.created_at DESC;
```

---

### Xem chi tiết đơn hàng

```sql
SELECT 
    o.id AS order_id,
    oi.drink_name_snapshot AS drink,
    oi.size_name_snapshot AS size,
    oi.quantity,
    oi.item_price,
    GROUP_CONCAT(oit.topping_name_snapshot) AS toppings
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN order_item_toppings oit ON oi.id = oit.order_item_id
WHERE o.id = 1
GROUP BY oi.id;
```

---

### Thống kê doanh thu theo ngày

```sql
SELECT 
    DATE(created_at) AS date,
    COUNT(*) AS total_orders,
    SUM(final_price) AS revenue
FROM orders
WHERE status = 'DONE'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

---

### Top món bán chạy

```sql
SELECT 
    d.name AS drink_name,
    COUNT(oi.id) AS times_ordered,
    SUM(oi.quantity) AS total_quantity
FROM order_items oi
JOIN drinks d ON oi.drink_id = d.id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'DONE'
GROUP BY d.id
ORDER BY total_quantity DESC
LIMIT 10;
```

---

### Kiểm tra mã giảm giá còn hiệu lực

```sql
SELECT 
    code,
    description,
    discount_type,
    discount_value,
    min_order_value,
    start_date,
    end_date
FROM promotions
WHERE is_active = 1
  AND NOW() BETWEEN start_date AND end_date;
```

---

## 🛠️ Quản lý Database

### Thêm user mới (Manual)

```sql
INSERT INTO users (username, phone, password, full_name, address, role, member_tier, points, active, is_blocked)
VALUES (
    'new_student',
    '0909999999',
    '$2a$10$...', -- BCrypt hashed password
    'Nguyen Van D',
    'KTX UTE',
    'USER',
    'BRONZE',
    0,
    TRUE,
    FALSE
);
```

**Lưu ý:** Password phải được hash bằng BCrypt. Dùng API `/api/auth/register` thay vì insert trực tiếp.

---

### Thêm món mới

```sql
INSERT INTO drinks (name, description, image_url, base_price, category_id, is_active)
VALUES (
    'Trà Sữa Dâu Tây',
    'Trà sữa vị dâu tây tươi mát',
    '/assets/drinks/milk_tea/tra_sua_dau_tay.png',
    48000,
    1,
    TRUE
);

-- Lấy ID món vừa thêm
SET @drink_id = LAST_INSERT_ID();

-- Thêm sizes
INSERT INTO drink_sizes (drink_id, size_name, extra_price)
VALUES 
    (@drink_id, 'M', 0),
    (@drink_id, 'L', 5000);
```

---

### Ẩn món (Soft delete)

```sql
UPDATE drinks 
SET is_active = FALSE 
WHERE id = 10;
```

---

### Cập nhật giá món

```sql
UPDATE drinks 
SET base_price = 35000 
WHERE id = 1;
```

---

### Thêm promotion mới

```sql
INSERT INTO promotions (code, description, discount_type, discount_value, start_date, end_date, min_order_value, is_active)
VALUES (
    'NEWYEAR2026',
    'Giảm 25% chào năm mới',
    'PERCENT',
    25.00,
    '2026-01-01 00:00:00',
    '2026-01-31 23:59:59',
    100000,
    TRUE
);
```

---

### Cập nhật trạng thái đơn hàng

```sql
UPDATE orders 
SET status = 'MAKING' 
WHERE id = 5 AND status = 'PENDING';
```

---

### Xóa dữ liệu test (CẢNH BÁO: Xóa vĩnh viễn)

```sql
-- Xóa tất cả orders (cascade sẽ xóa order_items và order_item_toppings)
DELETE FROM orders;

-- Reset AUTO_INCREMENT
ALTER TABLE orders AUTO_INCREMENT = 1;
```

---

## 💾 Backup & Restore

### Backup toàn bộ database

```bash
# Windows
mysqldump -u root -p LTDD_Thongtesst > backup_$(date +%Y%m%d).sql

# Mac/Linux
mysqldump -u root -p LTDD_Thongtesst > backup_$(date +%Y%m%d).sql
```

---

### Backup chỉ structure (không có data)

```bash
mysqldump -u root -p --no-data LTDD_Thongtesst > schema_only.sql
```

---

### Backup chỉ data (không có structure)

```bash
mysqldump -u root -p --no-create-info LTDD_Thongtesst > data_only.sql
```

---

### Restore database

```bash
# Tạo database mới
mysql -u root -p -e "CREATE DATABASE LTDD_Thongtesst_restore"

# Restore
mysql -u root -p LTDD_Thongtesst_restore < backup_20251127.sql
```

---

## 🔐 Security Best Practices

### 1. Không lưu password plain text

```sql
-- ❌ SAI
INSERT INTO users (username, password) VALUES ('user1', '123456');

-- ✅ ĐÚNG - Dùng API để register
POST /api/auth/register
{
  "username": "user1",
  "password": "123456"
}
```

---

### 2. Sử dụng prepared statements

```java
// ✅ ĐÚNG - JPA/Hibernate tự động dùng prepared statements
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);
```

---

### 3. Giới hạn quyền database user

```sql
-- Tạo user riêng cho application
CREATE USER 'utetea_app'@'localhost' IDENTIFIED BY 'strong_password';

-- Chỉ cấp quyền cần thiết
GRANT SELECT, INSERT, UPDATE, DELETE ON LTDD_Thongtesst.* TO 'utetea_app'@'localhost';

-- Không cấp quyền DROP, ALTER
```

---

## 📊 Monitoring & Maintenance

### Kiểm tra kích thước database

```sql
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'LTDD_Thongtesst'
GROUP BY table_schema;
```

---

### Kiểm tra số lượng records

```sql
SELECT 
    table_name,
    table_rows
FROM information_schema.tables
WHERE table_schema = 'LTDD_Thongtesst'
ORDER BY table_rows DESC;
```

---

### Optimize tables

```sql
OPTIMIZE TABLE drinks;
OPTIMIZE TABLE orders;
OPTIMIZE TABLE order_items;
```

---

### Kiểm tra indexes

```sql
SHOW INDEX FROM drinks;
SHOW INDEX FROM orders;
```

---

## 🐛 Troubleshooting Database

### Lỗi: Access denied

```sql
-- Kiểm tra user và quyền
SELECT user, host FROM mysql.user;
SHOW GRANTS FOR 'root'@'localhost';

-- Reset password root (nếu cần)
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

---

### Lỗi: Table doesn't exist

```sql
-- Kiểm tra tables có trong database
SHOW TABLES;

-- Kiểm tra structure của table
DESCRIBE drinks;
```

---

### Lỗi: Foreign key constraint fails

```sql
-- Kiểm tra foreign keys
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'LTDD_Thongtesst'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Tạm thời disable foreign key checks (CẢNH BÁO)
SET FOREIGN_KEY_CHECKS = 0;
-- ... thực hiện operations
SET FOREIGN_KEY_CHECKS = 1;
```

---

### Lỗi: Duplicate entry

```sql
-- Kiểm tra duplicate
SELECT username, COUNT(*) 
FROM users 
GROUP BY username 
HAVING COUNT(*) > 1;

-- Xóa duplicates (giữ lại record đầu tiên)
DELETE u1 FROM users u1
INNER JOIN users u2 
WHERE u1.id > u2.id 
  AND u1.username = u2.username;
```

---

## 📚 Tài liệu tham khảo

- **MySQL Documentation:** https://dev.mysql.com/doc/
- **JPA/Hibernate:** https://hibernate.org/orm/documentation/
- **Spring Data JPA:** https://spring.io/projects/spring-data-jpa

---

## 🆘 Cần hỗ trợ?

Nếu gặp vấn đề với database:
1. Kiểm tra MySQL service đang chạy
2. Kiểm tra connection string trong `application.properties`
3. Kiểm tra logs trong console
4. Thử kết nối bằng MySQL Workbench để test

---

**Database setup thành công! 🎉**

---

*Last updated: November 27, 2025*
