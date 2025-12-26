package com.utetea.backend.service;

import com.utetea.backend.dto.ChatResponse;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import com.utetea.backend.mapper.DrinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotService {

    private final DrinkRepository drinkRepository;
    private final DrinkCategoryRepository categoryRepository;
    private final PromotionRepository promotionRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final DrinkMapper drinkMapper;

    // ==================== INTENT PATTERNS ====================
    
    // Chào hỏi
    private static final List<String> GREETING_PATTERNS = Arrays.asList(
        "xin chào", "hello", "hi", "chào", "hey", "alo", "chào bạn", "xin chao",
        "good morning", "good afternoon", "chào buổi", "yo", "helu", "helo"
    );
    
    // Tạm biệt
    private static final List<String> GOODBYE_PATTERNS = Arrays.asList(
        "tạm biệt", "bye", "goodbye", "see you", "hẹn gặp lại", "chào nhé", "bye bye",
        "tạm biệt nhé", "gặp lại sau", "cảm ơn bye"
    );
    
    // Cảm ơn
    private static final List<String> THANKS_PATTERNS = Arrays.asList(
        "cảm ơn", "thank", "thanks", "cám ơn", "tks", "thankiu", "thank you",
        "cảm ơn bạn", "cảm ơn nhiều", "thanks a lot"
    );

    // Tìm kiếm đồ uống
    private static final List<String> DRINK_SEARCH_PATTERNS = Arrays.asList(
        "tìm", "search", "có gì", "muốn uống", "đồ uống", "thức uống", "menu", "món gì",
        "có món", "xem menu", "danh sách", "list", "tất cả món", "các món", "đồ uống gì"
    );
    
    // Hỏi giá
    private static final List<String> PRICE_PATTERNS = Arrays.asList(
        "giá", "bao nhiêu", "price", "cost", "tiền", "how much", "giá bao", "giá cả",
        "mắc không", "đắt không", "rẻ không", "giá tiền"
    );
    
    // Voucher/Khuyến mãi
    private static final List<String> VOUCHER_PATTERNS = Arrays.asList(
        "voucher", "khuyến mãi", "giảm giá", "mã giảm", "promotion", "ưu đãi", "sale",
        "discount", "coupon", "mã", "km", "promo", "deal", "giảm"
    );
    
    // Cửa hàng
    private static final List<String> STORE_PATTERNS = Arrays.asList(
        "cửa hàng", "store", "chi nhánh", "địa chỉ", "ở đâu", "gần đây", "location",
        "shop", "quán", "tiệm", "chỗ nào", "nơi nào", "vị trí"
    );
    
    // Đơn hàng
    private static final List<String> ORDER_PATTERNS = Arrays.asList(
        "đơn hàng", "order", "đặt hàng", "mua", "đơn của tôi", "my order", "lịch sử",
        "history", "đã đặt", "đơn gần đây"
    );
    
    // Gợi ý/Best seller
    private static final List<String> RECOMMEND_PATTERNS = Arrays.asList(
        "bán chạy", "best seller", "phổ biến", "hot", "nổi bật", "recommend", "gợi ý",
        "nên uống gì", "món ngon", "ngon nhất", "hay nhất", "top", "trending", "đề xuất",
        "uống gì ngon", "gì ngon", "món nào ngon", "suggest"
    );
    
    // Giúp đỡ
    private static final List<String> HELP_PATTERNS = Arrays.asList(
        "help", "giúp", "hướng dẫn", "làm sao", "cách", "how to", "hỗ trợ", "support",
        "bạn làm được gì", "có thể làm gì", "chức năng"
    );
    
    // Danh mục
    private static final List<String> CATEGORY_PATTERNS = Arrays.asList(
        "danh mục", "category", "loại", "thể loại", "nhóm", "phân loại", "loại đồ uống"
    );
    
    // Giờ mở cửa
    private static final List<String> HOURS_PATTERNS = Arrays.asList(
        "giờ mở cửa", "mở cửa", "đóng cửa", "giờ hoạt động", "opening", "hours",
        "mấy giờ mở", "mấy giờ đóng", "làm việc", "hoạt động"
    );
    
    // Thanh toán
    private static final List<String> PAYMENT_PATTERNS = Arrays.asList(
        "thanh toán", "payment", "trả tiền", "pay", "vnpay", "momo", "tiền mặt",
        "cash", "chuyển khoản", "thẻ", "card"
    );
    
    // Giao hàng
    private static final List<String> DELIVERY_PATTERNS = Arrays.asList(
        "giao hàng", "delivery", "ship", "shipper", "phí giao", "giao tận nơi",
        "đặt giao", "giao đến", "phí ship"
    );
    
    // Đồ uống cụ thể
    private static final List<String> TEA_KEYWORDS = Arrays.asList(
        "trà", "tea", "trà sữa", "milk tea", "trà đào", "trà vải", "trà xanh"
    );
    
    private static final List<String> COFFEE_KEYWORDS = Arrays.asList(
        "cà phê", "cafe", "coffee", "caphe", "espresso", "latte", "americano"
    );
    
    // Hỏi về app
    private static final List<String> APP_PATTERNS = Arrays.asList(
        "app", "ứng dụng", "phần mềm", "tải app", "download", "cài đặt"
    );
    
    // Phàn nàn/Góp ý
    private static final List<String> COMPLAINT_PATTERNS = Arrays.asList(
        "phàn nàn", "complaint", "góp ý", "feedback", "không hài lòng", "tệ", "dở",
        "chán", "không ngon", "đánh giá"
    );

    // ==================== MAIN PROCESS ====================
    
    public ChatResponse processMessage(String message, Long userId) {
        String lowerMessage = normalizeVietnamese(message.toLowerCase().trim());
        
        // Priority-based intent detection
        
        // 1. Chào hỏi
        if (containsAny(lowerMessage, GREETING_PATTERNS)) {
            return handleGreeting();
        }
        
        // 2. Tạm biệt
        if (containsAny(lowerMessage, GOODBYE_PATTERNS)) {
            return handleGoodbye();
        }
        
        // 3. Cảm ơn
        if (containsAny(lowerMessage, THANKS_PATTERNS)) {
            return handleThanks();
        }
        
        // 4. Giúp đỡ
        if (containsAny(lowerMessage, HELP_PATTERNS)) {
            return handleHelp();
        }
        
        // 5. Giờ mở cửa
        if (containsAny(lowerMessage, HOURS_PATTERNS)) {
            return handleOpeningHours();
        }
        
        // 6. Thanh toán
        if (containsAny(lowerMessage, PAYMENT_PATTERNS)) {
            return handlePaymentInfo();
        }
        
        // 7. Giao hàng
        if (containsAny(lowerMessage, DELIVERY_PATTERNS)) {
            return handleDeliveryInfo();
        }
        
        // 8. Voucher/Khuyến mãi
        if (containsAny(lowerMessage, VOUCHER_PATTERNS)) {
            return handleVoucherQuery();
        }
        
        // 9. Cửa hàng
        if (containsAny(lowerMessage, STORE_PATTERNS)) {
            return handleStoreQuery();
        }
        
        // 10. Đơn hàng
        if (containsAny(lowerMessage, ORDER_PATTERNS) && userId != null) {
            return handleOrderQuery(userId);
        }
        
        // 11. Gợi ý/Best seller
        if (containsAny(lowerMessage, RECOMMEND_PATTERNS)) {
            return handleRecommendation(lowerMessage);
        }
        
        // 12. Danh mục
        if (containsAny(lowerMessage, CATEGORY_PATTERNS)) {
            return handleCategoryQuery();
        }
        
        // 13. Hỏi giá (check trước search)
        if (containsAny(lowerMessage, PRICE_PATTERNS)) {
            return handlePriceQuery(lowerMessage);
        }
        
        // 14. Tìm kiếm đồ uống cụ thể (trà, cà phê)
        if (containsAny(lowerMessage, TEA_KEYWORDS)) {
            return handleDrinkSearch(lowerMessage, "trà");
        }
        
        if (containsAny(lowerMessage, COFFEE_KEYWORDS)) {
            return handleDrinkSearch(lowerMessage, "cà phê");
        }
        
        // 15. Tìm kiếm chung
        if (containsAny(lowerMessage, DRINK_SEARCH_PATTERNS)) {
            return handleDrinkSearch(lowerMessage, null);
        }
        
        // 16. App info
        if (containsAny(lowerMessage, APP_PATTERNS)) {
            return handleAppInfo();
        }
        
        // 17. Phàn nàn
        if (containsAny(lowerMessage, COMPLAINT_PATTERNS)) {
            return handleComplaint();
        }
        
        // 18. Fallback - thử tìm kiếm theo từ khóa
        return handleSmartSearch(lowerMessage);
    }

    // ==================== HANDLERS ====================
    
    private ChatResponse handleGreeting() {
        String timeGreeting = getTimeBasedGreeting();
        String[] greetings = {
            timeGreeting + "! 👋 Tôi là trợ lý ảo của UTE Tea.\n\n" +
                "Tôi có thể giúp bạn:\n" +
                "🍵 Tìm kiếm đồ uống yêu thích\n" +
                "💰 Xem giá và khuyến mãi\n" +
                "📍 Tìm cửa hàng gần bạn\n" +
                "📦 Kiểm tra đơn hàng\n\n" +
                "Bạn muốn tôi giúp gì nào? 😊",
            
            timeGreeting + "! 🌟 Chào mừng bạn đến với UTE Tea!\n\n" +
                "Hôm nay bạn muốn thưởng thức gì?\n" +
                "• Gõ \"menu\" để xem tất cả đồ uống\n" +
                "• Gõ \"gợi ý\" để xem món hot\n" +
                "• Gõ \"khuyến mãi\" để xem ưu đãi\n\n" +
                "Tôi sẵn sàng phục vụ bạn! ☕"
        };
        return ChatResponse.builder()
            .message(greetings[new Random().nextInt(greetings.length)])
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleGoodbye() {
        String[] goodbyes = {
            "Tạm biệt bạn! 👋 Hẹn gặp lại lần sau nhé! Chúc bạn một ngày tuyệt vời! 🌟",
            "Bye bye! 😊 Cảm ơn bạn đã ghé thăm UTE Tea. Mong sớm được phục vụ bạn! ☕",
            "Chào bạn nhé! 🍵 Đừng quên ghé lại UTE Tea khi khát nha! 💚",
            "Tạm biệt! Chúc bạn ngon miệng với đồ uống của UTE Tea! 🧋"
        };
        return ChatResponse.builder()
            .message(goodbyes[new Random().nextInt(goodbyes.length)])
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleThanks() {
        String[] thanks = {
            "Không có gì ạ! 😊 Rất vui được giúp bạn! Còn gì cần hỗ trợ không?",
            "Dạ không có chi! 💚 UTE Tea luôn sẵn sàng phục vụ bạn!",
            "Cảm ơn bạn đã tin tưởng UTE Tea! 🍵 Chúc bạn ngon miệng!",
            "Rất vui vì đã giúp được bạn! 🌟 Hãy quay lại khi cần nhé!"
        };
        return ChatResponse.builder()
            .message(thanks[new Random().nextInt(thanks.length)])
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleHelp() {
        return ChatResponse.builder()
            .message("📚 **Hướng dẫn sử dụng trợ lý UTE Tea**\n\n" +
                "🔍 **Tìm đồ uống:**\n" +
                "• \"Tìm trà sữa\" hoặc \"có trà đào không?\"\n" +
                "• \"Menu\" để xem tất cả\n\n" +
                "💰 **Xem giá:**\n" +
                "• \"Giá trà sữa trân châu bao nhiêu?\"\n\n" +
                "🎁 **Khuyến mãi:**\n" +
                "• \"Có voucher gì không?\" hoặc \"khuyến mãi\"\n\n" +
                "📍 **Cửa hàng:**\n" +
                "• \"Cửa hàng ở đâu?\" hoặc \"địa chỉ\"\n\n" +
                "📦 **Đơn hàng:**\n" +
                "• \"Xem đơn hàng của tôi\"\n\n" +
                "⭐ **Gợi ý:**\n" +
                "• \"Món nào ngon?\" hoặc \"gợi ý đi\"\n\n" +
                "Hãy thử hỏi tôi bất cứ điều gì! 😊")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleOpeningHours() {
        return ChatResponse.builder()
            .message("🕐 **Giờ hoạt động UTE Tea**\n\n" +
                "📅 Thứ 2 - Chủ nhật\n" +
                "⏰ 7:00 sáng - 21:30 tối\n\n" +
                "💡 Lưu ý:\n" +
                "• Đặt hàng online: 7:00 - 21:00\n" +
                "• Giao hàng: 7:30 - 21:30\n" +
                "• Lấy tại quán: 7:00 - 21:30\n\n" +
                "Bạn muốn đặt hàng ngay không? 🍵")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handlePaymentInfo() {
        return ChatResponse.builder()
            .message("💳 **Phương thức thanh toán**\n\n" +
                "UTE Tea hỗ trợ các hình thức:\n\n" +
                "💵 **Tiền mặt (COD)**\n" +
                "• Thanh toán khi nhận hàng\n\n" +
                "📱 **VNPay**\n" +
                "• Quét QR thanh toán nhanh\n" +
                "• Liên kết thẻ ngân hàng\n\n" +
                "🔒 Mọi giao dịch đều được bảo mật!\n\n" +
                "Bạn muốn đặt hàng ngay không? 🛒")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleDeliveryInfo() {
        return ChatResponse.builder()
            .message("🚚 **Thông tin giao hàng**\n\n" +
                "📍 **Phạm vi giao hàng:**\n" +
                "• Bán kính 5km từ cửa hàng\n\n" +
                "💰 **Phí giao hàng:**\n" +
                "• Dưới 2km: 10.000đ\n" +
                "• 2-5km: 15.000đ\n" +
                "• FREE ship cho đơn từ 100.000đ\n\n" +
                "⏱️ **Thời gian giao:**\n" +
                "• Trung bình 20-30 phút\n\n" +
                "💡 Tip: Đặt trước 30 phút để nhận hàng đúng giờ!\n\n" +
                "Gõ \"cửa hàng\" để xem địa chỉ gần bạn! 📍")
            .type("TEXT")
            .build();
    }

    private ChatResponse handleAppInfo() {
        return ChatResponse.builder()
            .message("📱 **Ứng dụng UTE Tea**\n\n" +
                "Tính năng nổi bật:\n" +
                "✅ Đặt hàng nhanh chóng\n" +
                "✅ Thanh toán online an toàn\n" +
                "✅ Theo dõi đơn hàng realtime\n" +
                "✅ Tích điểm đổi quà\n" +
                "✅ Nhận thông báo khuyến mãi\n" +
                "✅ Đặt hàng bằng giọng nói\n\n" +
                "🎁 Ưu đãi cho người dùng mới!\n\n" +
                "Bạn cần hỗ trợ gì thêm không? 😊")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleComplaint() {
        return ChatResponse.builder()
            .message("😔 Rất tiếc vì bạn chưa hài lòng!\n\n" +
                "UTE Tea luôn lắng nghe góp ý của bạn.\n\n" +
                "📞 **Hotline:** 1900-xxxx\n" +
                "📧 **Email:** support@utetea.com\n" +
                "💬 **Fanpage:** fb.com/utetea\n\n" +
                "Hoặc bạn có thể mô tả vấn đề ngay tại đây, " +
                "chúng tôi sẽ ghi nhận và cải thiện! 🙏\n\n" +
                "Cảm ơn bạn đã góp ý! 💚")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleCategoryQuery() {
        List<DrinkCategory> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        if (categories.isEmpty()) {
            return ChatResponse.builder()
                .message("Xin lỗi, hiện tại chưa có thông tin danh mục.")
                .type("TEXT")
                .build();
        }
        
        StringBuilder sb = new StringBuilder("📋 **Danh mục đồ uống UTE Tea**\n\n");
        for (int i = 0; i < categories.size(); i++) {
            DrinkCategory cat = categories.get(i);
            sb.append(getEmojiForCategory(cat.getName()))
              .append(" **").append(cat.getName()).append("**\n");
            if (cat.getDescription() != null && !cat.getDescription().isEmpty()) {
                sb.append("   ").append(cat.getDescription()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("💡 Gõ tên danh mục để xem chi tiết!\n");
        sb.append("Ví dụ: \"trà sữa\" hoặc \"cà phê\"");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("CATEGORIES")
            .data(categories)
            .build();
    }
    
    private ChatResponse handleRecommendation(String message) {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Lọc theo context nếu có
        String context = extractContext(message);
        if (context != null) {
            final String ctx = context;
            drinks = drinks.stream()
                .filter(d -> d.getName().toLowerCase().contains(ctx) ||
                            (d.getCategory() != null && 
                             d.getCategory().getName().toLowerCase().contains(ctx)))
                .collect(Collectors.toList());
        }
        
        if (drinks.isEmpty()) {
            return ChatResponse.builder()
                .message("Xin lỗi, hiện tại chưa có món phù hợp. Bạn thử tìm món khác nhé!")
                .type("TEXT")
                .build();
        }
        
        // Random 5 món làm "best seller"
        Collections.shuffle(drinks);
        List<Drink> recommended = drinks.stream().limit(5).collect(Collectors.toList());
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder("🔥 **Món hot được yêu thích!**\n\n");
        for (int i = 0; i < recommended.size(); i++) {
            Drink d = recommended.get(i);
            sb.append(getMedalEmoji(i + 1))
              .append(" **").append(d.getName()).append("**\n")
              .append("   💰 ").append(formatPrice(d.getBasePrice())).append("\n");
            if (d.getDescription() != null && !d.getDescription().isEmpty()) {
                String desc = d.getDescription().length() > 50 
                    ? d.getDescription().substring(0, 50) + "..." 
                    : d.getDescription();
                sb.append("   📝 ").append(desc).append("\n");
            }
            sb.append("\n");
        }
        sb.append("💡 Gõ tên món để xem chi tiết và đặt hàng!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }

    private ChatResponse handleDrinkSearch(String message, String category) {
        String searchTerm = extractSearchTerm(message);
        List<Drink> drinks;
        
        if (category != null) {
            // Tìm theo category - sử dụng JOIN FETCH
            final String cat = category;
            drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory().stream()
                .filter(d -> d.getName().toLowerCase().contains(cat) ||
                            (d.getCategory() != null && 
                             d.getCategory().getName().toLowerCase().contains(cat)))
                .collect(Collectors.toList());
        } else if (searchTerm.isEmpty() || searchTerm.equals("menu")) {
            // Hiển thị tất cả - sử dụng JOIN FETCH
            drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        } else {
            // Tìm theo từ khóa - sử dụng JOIN FETCH
            drinks = drinkRepository.searchByNameWithSizesAndCategory(searchTerm);
            
            // Nếu không tìm thấy, thử tìm trong description
            if (drinks.isEmpty()) {
                final String term = searchTerm;
                drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory().stream()
                    .filter(d -> (d.getDescription() != null && 
                                 d.getDescription().toLowerCase().contains(term)))
                    .collect(Collectors.toList());
            }
        }
        
        if (drinks.isEmpty()) {
            return ChatResponse.builder()
                .message("😅 Xin lỗi, tôi không tìm thấy \"" + searchTerm + "\".\n\n" +
                    "💡 Gợi ý:\n" +
                    "• Thử từ khóa khác như \"trà sữa\", \"cà phê\"\n" +
                    "• Gõ \"menu\" để xem tất cả đồ uống\n" +
                    "• Gõ \"gợi ý\" để xem món hot")
                .type("TEXT")
                .build();
        }
        
        // Giới hạn kết quả
        if (drinks.size() > 10) {
            drinks = drinks.subList(0, 10);
        }
        
        List<DrinkDto> drinkDtos = drinks.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        if (category != null) {
            sb.append("🍵 **").append(capitalizeFirst(category)).append("**\n\n");
        } else if (searchTerm.isEmpty() || searchTerm.equals("menu")) {
            sb.append("📋 **Menu UTE Tea**\n\n");
        } else {
            sb.append("🔍 Kết quả tìm kiếm \"").append(searchTerm).append("\":\n\n");
        }
        
        for (Drink d : drinks) {
            sb.append("🧋 **").append(d.getName()).append("**\n");
            sb.append("   💰 ").append(formatPrice(d.getBasePrice()));
            if (d.getSizes() != null && !d.getSizes().isEmpty()) {
                sb.append(" (Size M)");
            }
            sb.append("\n\n");
        }
        
        if (drinks.size() == 10) {
            sb.append("📌 Hiển thị 10 kết quả đầu tiên.\n");
        }
        sb.append("💡 Gõ tên món để xem chi tiết!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handlePriceQuery(String message) {
        String searchTerm = extractSearchTerm(message);
        
        if (searchTerm.isEmpty()) {
            return ChatResponse.builder()
                .message("💰 Bạn muốn hỏi giá món nào ạ?\n\n" +
                    "Ví dụ:\n" +
                    "• \"Giá trà sữa trân châu\"\n" +
                    "• \"Bao nhiêu tiền cà phê sữa?\"\n\n" +
                    "Hoặc gõ \"menu\" để xem tất cả giá!")
                .type("TEXT")
                .build();
        }
        
        // Sử dụng JOIN FETCH để load sizes
        List<Drink> drinks = drinkRepository.searchByNameWithSizesAndCategory(searchTerm);
        
        if (drinks.isEmpty()) {
            return ChatResponse.builder()
                .message("😅 Không tìm thấy món \"" + searchTerm + "\".\n\n" +
                    "Bạn thử:\n" +
                    "• Kiểm tra lại tên món\n" +
                    "• Gõ \"menu\" để xem danh sách")
                .type("TEXT")
                .build();
        }
        
        List<DrinkDto> drinkDtos = drinks.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder("💰 **Bảng giá**\n\n");
        for (Drink drink : drinks) {
            sb.append("🧋 **").append(drink.getName()).append("**\n");
            sb.append("   Giá: ").append(formatPrice(drink.getBasePrice()));
            
            if (drink.getSizes() != null && !drink.getSizes().isEmpty()) {
                sb.append(" (Size M)\n");
                sb.append("   📏 Size:\n");
                for (DrinkSize size : drink.getSizes()) {
                    double totalPrice = drink.getBasePrice().doubleValue() + 
                                       size.getExtraPrice().doubleValue();
                    sb.append("      • ").append(size.getSizeName())
                      .append(": ").append(formatPrice(totalPrice)).append("\n");
                }
            } else {
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("🛒 Bạn muốn đặt món nào?");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }

    private ChatResponse handleVoucherQuery() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(now, now);
        
        if (promotions.isEmpty()) {
            return ChatResponse.builder()
                .message("😔 Hiện tại không có khuyến mãi nào.\n\n" +
                    "💡 Nhưng đừng lo!\n" +
                    "• Theo dõi app để nhận thông báo ưu đãi mới\n" +
                    "• Đăng ký thành viên để tích điểm đổi quà\n\n" +
                    "Bạn muốn xem menu không? Gõ \"menu\" nhé! 🍵")
                .type("TEXT")
                .build();
        }
        
        StringBuilder sb = new StringBuilder("🎉 **Khuyến mãi đang có!**\n\n");
        for (Promotion p : promotions) {
            sb.append("🎫 **").append(p.getCode()).append("**\n");
            if (p.getDescription() != null) {
                sb.append("   📝 ").append(p.getDescription()).append("\n");
            }
            
            if (p.getDiscountType() == DiscountType.PERCENT) {
                sb.append("   💰 Giảm ").append(p.getDiscountValue()).append("%");
                if (p.getMaxDiscountAmount() != null) {
                    sb.append(" (tối đa ").append(formatPrice(p.getMaxDiscountAmount())).append(")");
                }
            } else {
                sb.append("   💰 Giảm ").append(formatPrice(p.getDiscountValue()));
            }
            sb.append("\n");
            
            if (p.getMinOrderValue() != null && p.getMinOrderValue().doubleValue() > 0) {
                sb.append("   🛒 Đơn tối thiểu: ").append(formatPrice(p.getMinOrderValue())).append("\n");
            }
            
            sb.append("   ⏰ HSD: ").append(formatDate(p.getEndDate())).append("\n\n");
        }
        
        sb.append("💡 Nhập mã khi thanh toán để được giảm giá!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("VOUCHERS")
            .data(promotions)
            .build();
    }
    
    private ChatResponse handleStoreQuery() {
        List<Store> stores = storeRepository.findAll();
        
        if (stores.isEmpty()) {
            return ChatResponse.builder()
                .message("😔 Xin lỗi, hiện tại chưa có thông tin cửa hàng.")
                .type("TEXT")
                .build();
        }
        
        StringBuilder sb = new StringBuilder("📍 **Cửa hàng UTE Tea**\n\n");
        for (Store store : stores) {
            sb.append("🏪 **").append(store.getStoreName()).append("**\n");
            sb.append("   📍 ").append(store.getAddress()).append("\n");
            
            if (store.getPhone() != null) {
                sb.append("   📞 ").append(store.getPhone()).append("\n");
            }
            
            if (store.getOpenTime() != null && store.getCloseTime() != null) {
                sb.append("   🕐 ").append(store.getOpenTime())
                  .append(" - ").append(store.getCloseTime()).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("💡 Mở app để xem bản đồ và chỉ đường!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("STORES")
            .data(stores)
            .build();
    }
    
    private ChatResponse handleOrderQuery(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (orders.isEmpty()) {
            return ChatResponse.builder()
                .message("📦 Bạn chưa có đơn hàng nào.\n\n" +
                    "Hãy đặt món ngay để thưởng thức đồ uống tuyệt vời! 🍵\n\n" +
                    "💡 Gõ \"menu\" để xem đồ uống\n" +
                    "💡 Gõ \"gợi ý\" để xem món hot")
                .type("TEXT")
                .build();
        }
        
        List<Order> recentOrders = orders.stream().limit(5).collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder("📦 **Đơn hàng của bạn**\n\n");
        for (Order order : recentOrders) {
            sb.append("🧾 **Đơn #").append(order.getId()).append("**\n");
            sb.append("   ").append(translateStatus(order.getStatus())).append("\n");
            sb.append("   💰 ").append(formatPrice(order.getFinalPrice())).append("\n");
            sb.append("   📅 ").append(formatDateTime(order.getCreatedAt())).append("\n\n");
        }
        
        if (orders.size() > 5) {
            sb.append("📌 Hiển thị 5 đơn gần nhất.\n");
        }
        sb.append("💡 Mở app để xem chi tiết đơn hàng!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("ORDER")
            .data(recentOrders)
            .build();
    }

    private ChatResponse handleSmartSearch(String message) {
        // Thử tìm trong database - sử dụng JOIN FETCH
        List<Drink> drinks = drinkRepository.searchByNameWithSizesAndCategory(message);
        
        if (!drinks.isEmpty()) {
            List<DrinkDto> drinkDtos = drinks.stream()
                .map(drinkMapper::toDto)
                .collect(Collectors.toList());
            
            StringBuilder sb = new StringBuilder("🔍 Tôi tìm thấy:\n\n");
            for (Drink d : drinks) {
                sb.append("🧋 **").append(d.getName()).append("**\n");
                sb.append("   💰 ").append(formatPrice(d.getBasePrice())).append("\n\n");
            }
            sb.append("Bạn muốn đặt món nào?");
            
            return ChatResponse.builder()
                .message(sb.toString())
                .type("DRINKS")
                .data(drinkDtos)
                .build();
        }
        
        // Tìm trong category
        List<DrinkCategory> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        for (DrinkCategory cat : categories) {
            if (cat.getName().toLowerCase().contains(message)) {
                return handleDrinkSearch(message, cat.getName().toLowerCase());
            }
        }
        
        // Fallback với gợi ý thông minh
        return ChatResponse.builder()
            .message("🤔 Hmm, tôi chưa hiểu ý bạn lắm.\n\n" +
                "Bạn có thể thử:\n" +
                "• 🍵 \"Tìm trà sữa\" - tìm đồ uống\n" +
                "• 💰 \"Giá cà phê\" - xem giá\n" +
                "• 🎁 \"Khuyến mãi\" - xem ưu đãi\n" +
                "• 📍 \"Cửa hàng\" - tìm địa chỉ\n" +
                "• ⭐ \"Gợi ý\" - món hot\n" +
                "• ❓ \"Help\" - hướng dẫn\n\n" +
                "Hoặc gõ trực tiếp tên món bạn muốn tìm! 😊")
            .type("TEXT")
            .build();
    }
    
    // ==================== HELPER METHODS ====================
    
    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
    
    private String normalizeVietnamese(String text) {
        // Chuẩn hóa một số từ viết tắt/sai chính tả phổ biến
        return text
            .replace("ko", "không")
            .replace("k ", "không ")
            .replace("dc", "được")
            .replace("đc", "được")
            .replace("bn", "bao nhiêu")
            .replace("j", "gì")
            .replace("z", "gì")
            .replace("vs", "với")
            .replace("mk", "mình")
            .replace("ck", "chồng")
            .replace("vk", "vợ");
    }
    
    private String extractSearchTerm(String message) {
        String[] removeWords = {
            "tìm", "kiếm", "search", "có", "muốn", "uống", "giá", "bao nhiêu",
            "price", "món", "đồ uống", "thức uống", "cho", "tôi", "xem", "của",
            "không", "nào", "gì", "ạ", "nhé", "đi", "với", "và", "hay", "hoặc",
            "cái", "ly", "cốc", "một", "hai", "ba", "bốn", "năm"
        };
        
        String result = message.toLowerCase();
        for (String word : removeWords) {
            result = result.replace(word, " ");
        }
        return result.replaceAll("\\s+", " ").trim();
    }
    
    private String extractContext(String message) {
        if (message.contains("trà")) return "trà";
        if (message.contains("cà phê") || message.contains("cafe") || message.contains("coffee")) return "cà phê";
        if (message.contains("sữa")) return "sữa";
        if (message.contains("đá")) return "đá";
        return null;
    }
    
    private String getTimeBasedGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Chào buổi sáng";
        if (hour < 18) return "Chào buổi chiều";
        return "Chào buổi tối";
    }
    
    private String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "0đ";
        return String.format("%,.0fđ", price.doubleValue());
    }
    
    private String formatPrice(double price) {
        return String.format("%,.0fđ", price);
    }
    
    private String formatDate(LocalDateTime date) {
        if (date == null) return "";
        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
    }
    
    private String formatDateTime(java.time.Instant instant) {
        if (instant == null) return "";
        LocalDateTime date = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return String.format("%02d:%02d %02d/%02d/%d",
            date.getHour(), date.getMinute(),
            date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
    
    private String translateStatus(OrderStatus status) {
        if (status == null) return "❓ Không xác định";
        switch (status) {
            case PENDING: return "⏳ Chờ xác nhận";
            case MAKING: return "👨‍🍳 Đang pha chế";
            case SHIPPING: return "🚚 Đang giao hàng";
            case READY: return "✅ Sẵn sàng lấy";
            case DONE: return "🎉 Hoàn thành";
            case CANCELED: return "❌ Đã hủy";
            default: return "❓ " + status.name();
        }
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String getMedalEmoji(int rank) {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "⭐";
        }
    }
    
    private String getEmojiForCategory(String categoryName) {
        if (categoryName == null) return "🍵";
        String lower = categoryName.toLowerCase();
        if (lower.contains("trà sữa") || lower.contains("milk tea")) return "🧋";
        if (lower.contains("trà")) return "🍵";
        if (lower.contains("cà phê") || lower.contains("coffee")) return "☕";
        if (lower.contains("sinh tố") || lower.contains("smoothie")) return "🥤";
        if (lower.contains("nước ép") || lower.contains("juice")) return "🧃";
        if (lower.contains("đá xay")) return "🧊";
        return "🍹";
    }
}
