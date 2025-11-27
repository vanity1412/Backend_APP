HƯỚNG DẪN SỬ DỤNG FOLDER ASSETS
================================

Folder này chứa hình ảnh món nước cho UTE Tea theo đúng cấu trúc database.

CẤU TRÚC:
---------
assets/
  └── drinks/
      ├── milk_tea/          (Trà sữa Houjicha)
      │   ├── ute_houjicha_classic.png
      │   ├── houjicha_kem_cheese.png
      │   ├── houjicha_tc_duongden.png
      │   └── matcha_houjicha_mix.png
      │
      ├── fruit_tea/         (Trà trái cây)
      │   ├── dao_hong_ute.png
      │   ├── travai_nhai_tuoi.png
      │   ├── cam_sa_mat_ong.png
      │   ├── kiwi_nhiet_doi.png
      │   └── xoai_kem_tuyet.png
      │
      ├── macchiato/         (Macchiato)
      │   ├── hong_tra_macchiato.png
      │   ├── tra_xanh_kem_cheese_ute.png
      │   └── oolong_kem_sua.png
      │
      └── special/           (Đặc biệt)
          ├── ute_galaxy_tea.png
          ├── ute_brown_sugar_latte.png
          ├── matcha_daxay_ute.png
          └── cookie_cream_frappe.png

CÁCH DÙNG:
----------
1. Đặt file ảnh vào đúng folder theo loại món
2. Tên file phải CHÍNH XÁC như trong database
3. Định dạng: PNG (như database đang dùng)

VÍ DỤ:
------
- Database: /assets/drinks/milk_tea/ute_houjicha_classic.png
- File thực tế: assets/drinks/milk_tea/ute_houjicha_classic.png
- URL truy cập: http://localhost:8080/assets/drinks/milk_tea/ute_houjicha_classic.png

DANH SÁCH ẢNH CẦN CÓ (16 món):
-------------------------------
Milk Tea (4 món):
  ✓ ute_houjicha_classic.png
  ✓ houjicha_kem_cheese.png
  ✓ houjicha_tc_duongden.png
  ✓ matcha_houjicha_mix.png

Fruit Tea (5 món):
  ✓ dao_hong_ute.png
  ✓ travai_nhai_tuoi.png
  ✓ cam_sa_mat_ong.png
  ✓ kiwi_nhiet_doi.png
  ✓ xoai_kem_tuyet.png

Macchiato (3 món):
  ✓ hong_tra_macchiato.png
  ✓ tra_xanh_kem_cheese_ute.png
  ✓ oolong_kem_sua.png

Special (4 món):
  ✓ ute_galaxy_tea.png
  ✓ ute_brown_sugar_latte.png
  ✓ matcha_daxay_ute.png
  ✓ cookie_cream_frappe.png

LƯU Ý:
------
- Định dạng: PNG (theo database)
- Kích thước khuyến nghị: 300x300px hoặc 500x500px
- Dung lượng: < 500KB mỗi ảnh
- Tên file PHẢI CHÍNH XÁC như database (không dấu, viết thường)

KHI DEPLOY LÊN SERVER:
----------------------
- Copy toàn bộ folder assets/ lên server
- Đảm bảo đường dẫn assets/ ngang hàng với file .jar
- Hoặc cấu hình đường dẫn tuyệt đối trong application.properties
