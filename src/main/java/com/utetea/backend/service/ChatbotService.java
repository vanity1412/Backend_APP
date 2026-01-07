package com.utetea.backend.service;

import com.utetea.backend.dto.ChatResponse;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.dto.WeatherDto;
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
    private final WeatherService weatherService;
    private final GeminiService geminiService;
    
    // Cache thời tiết để tránh gọi API quá nhiều
    private WeatherDto cachedWeather;
    private long weatherCacheTime = 0;
    
    // Flag để bật/tắt Gemini AI
    private static final boolean USE_GEMINI_AI = true;
    private static final long WEATHER_CACHE_DURATION = 30 * 60 * 1000; // 30 phút

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
    
    // ==================== MOOD PATTERNS ====================
    
    // Hỏi tâm trạng
    private static final List<String> MOOD_ASK_PATTERNS = Arrays.asList(
        "hôm nay thế nào", "cảm thấy", "tâm trạng", "mood", "feeling", "bạn thế nào",
        "hôm nay sao", "đang buồn", "đang vui", "đang mệt", "thấy sao"
    );
    
    // Mệt mỏi / Stress
    private static final List<String> MOOD_TIRED_PATTERNS = Arrays.asList(
        "mệt", "tired", "stress", "căng thẳng", "áp lực", "kiệt sức", "uể oải",
        "buồn ngủ", "sleepy", "exhausted", "mệt mỏi", "chán nản", "không có năng lượng"
    );
    
    // Vui vẻ / Hạnh phúc
    private static final List<String> MOOD_HAPPY_PATTERNS = Arrays.asList(
        "vui", "happy", "hạnh phúc", "phấn khởi", "excited", "tuyệt vời", "great",
        "good", "tốt", "khỏe", "fine", "ok", "ổn", "hào hứng", "sung sướng"
    );
    
    // Buồn / Chán
    private static final List<String> MOOD_SAD_PATTERNS = Arrays.asList(
        "buồn", "sad", "chán", "bored", "lonely", "cô đơn", "tệ", "bad", "down",
        "không vui", "thất vọng", "disappointed", "depressed", "u sầu"
    );
    
    // Nóng / Thời tiết
    private static final List<String> MOOD_HOT_PATTERNS = Arrays.asList(
        "nóng", "hot", "nắng", "sunny", "oi bức", "nóng quá", "trời nóng",
        "hè", "summer", "khát", "thirsty", "cần mát"
    );
    
    // Lạnh
    private static final List<String> MOOD_COLD_PATTERNS = Arrays.asList(
        "lạnh", "cold", "rét", "se lạnh", "mùa đông", "winter", "ấm", "warm"
    );
    
    // Cần năng lượng / Tỉnh táo
    private static final List<String> MOOD_ENERGY_PATTERNS = Arrays.asList(
        "cần năng lượng", "energy", "tỉnh táo", "awake", "focus", "tập trung",
        "làm việc", "học bài", "study", "work", "cần tỉnh", "buồn ngủ quá"
    );
    
    // Thư giãn
    private static final List<String> MOOD_RELAX_PATTERNS = Arrays.asList(
        "thư giãn", "relax", "chill", "nghỉ ngơi", "rest", "nhẹ nhàng", "calm",
        "bình tĩnh", "peaceful", "yên bình"
    );
    
    // Gợi ý theo thời tiết
    private static final List<String> WEATHER_PATTERNS = Arrays.asList(
        "thời tiết", "weather", "trời", "hôm nay trời", "ngoài trời", "nhiệt độ",
        "gợi ý theo thời tiết", "uống gì hôm nay", "hôm nay uống gì", "nên uống gì"
    );
    
    // Hỏi về size/topping
    private static final List<String> SIZE_PATTERNS = Arrays.asList(
        "size", "kích cỡ", "cỡ", "nhỏ", "vừa", "lớn", "s", "m", "l"
    );
    
    private static final List<String> TOPPING_PATTERNS = Arrays.asList(
        "topping", "thêm", "trân châu", "pudding", "thạch", "kem", "cheese",
        "foam", "đường đen", "sương sáo"
    );

    // ==================== MAIN PROCESS ====================
    
    public ChatResponse processMessage(String message, Long userId) {
        if (message == null || message.trim().isEmpty()) {
            return handleEmptyMessage();
        }
        
        String lowerMessage = normalizeVietnamese(message.toLowerCase().trim());
        
        // Kiểm tra độ dài tin nhắn
        if (lowerMessage.length() > 500) {
            return handleTooLongMessage();
        }
        
        // Phân tích ý định với scoring system
        IntentScore bestIntent = analyzeIntent(lowerMessage, userId);
        
        // Xử lý theo ý định có điểm cao nhất
        return executeIntent(bestIntent, lowerMessage, userId);
    }
    
    // Phân tích ý định với hệ thống điểm số
    private IntentScore analyzeIntent(String message, Long userId) {
        List<IntentScore> scores = new ArrayList<>();
        
        // Tính điểm cho từng ý định
        scores.add(new IntentScore("GREETING", calculateScore(message, GREETING_PATTERNS), 1.0));
        scores.add(new IntentScore("GOODBYE", calculateScore(message, GOODBYE_PATTERNS), 1.0));
        scores.add(new IntentScore("THANKS", calculateScore(message, THANKS_PATTERNS), 1.0));
        
        // Mood patterns có trọng số cao hơn
        scores.add(new IntentScore("MOOD_ASK", calculateScore(message, MOOD_ASK_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_TIRED", calculateScore(message, MOOD_TIRED_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_HAPPY", calculateScore(message, MOOD_HAPPY_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_SAD", calculateScore(message, MOOD_SAD_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_HOT", calculateScore(message, MOOD_HOT_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_COLD", calculateScore(message, MOOD_COLD_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_ENERGY", calculateScore(message, MOOD_ENERGY_PATTERNS), 1.2));
        scores.add(new IntentScore("MOOD_RELAX", calculateScore(message, MOOD_RELAX_PATTERNS), 1.2));
        
        // Weather-based recommendation - ưu tiên cao
        scores.add(new IntentScore("WEATHER_RECOMMEND", calculateScore(message, WEATHER_PATTERNS), 1.5));
        scores.add(new IntentScore("SIZE_INFO", calculateScore(message, SIZE_PATTERNS), 1.0));
        scores.add(new IntentScore("TOPPING_INFO", calculateScore(message, TOPPING_PATTERNS), 1.0));
        
        // Business intents
        scores.add(new IntentScore("HELP", calculateScore(message, HELP_PATTERNS), 1.0));
        scores.add(new IntentScore("HOURS", calculateScore(message, HOURS_PATTERNS), 1.0));
        scores.add(new IntentScore("PAYMENT", calculateScore(message, PAYMENT_PATTERNS), 1.0));
        scores.add(new IntentScore("DELIVERY", calculateScore(message, DELIVERY_PATTERNS), 1.0));
        scores.add(new IntentScore("VOUCHER", calculateScore(message, VOUCHER_PATTERNS), 1.0));
        scores.add(new IntentScore("STORE", calculateScore(message, STORE_PATTERNS), 1.0));
        
        // Order query chỉ có ý nghĩa khi có userId
        if (userId != null) {
            scores.add(new IntentScore("ORDER", calculateScore(message, ORDER_PATTERNS), 1.0));
        }
        
        scores.add(new IntentScore("RECOMMEND", calculateScore(message, RECOMMEND_PATTERNS), 1.0));
        scores.add(new IntentScore("CATEGORY", calculateScore(message, CATEGORY_PATTERNS), 1.0));
        
        // Price query có trọng số cao khi có từ khóa giá
        double priceScore = calculateScore(message, PRICE_PATTERNS);
        if (priceScore > 0 && containsProductKeywords(message)) {
            priceScore *= 1.3; // Tăng trọng số khi có cả từ khóa sản phẩm
        }
        scores.add(new IntentScore("PRICE", priceScore, 1.0));
        
        // Drink search
        scores.add(new IntentScore("TEA_SEARCH", calculateScore(message, TEA_KEYWORDS), 1.1));
        scores.add(new IntentScore("COFFEE_SEARCH", calculateScore(message, COFFEE_KEYWORDS), 1.1));
        scores.add(new IntentScore("DRINK_SEARCH", calculateScore(message, DRINK_SEARCH_PATTERNS), 0.9));
        
        scores.add(new IntentScore("APP", calculateScore(message, APP_PATTERNS), 1.0));
        scores.add(new IntentScore("COMPLAINT", calculateScore(message, COMPLAINT_PATTERNS), 1.0));
        
        // Tìm intent có điểm cao nhất
        return scores.stream()
            .filter(s -> s.getFinalScore() > 0)
            .max(Comparator.comparing(IntentScore::getFinalScore))
            .orElse(new IntentScore("SMART_SEARCH", 0.1, 1.0));
    }
    
    // Tính điểm cho pattern matching
    private double calculateScore(String message, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) return 0.0;
        
        double score = 0.0;
        int matchCount = 0;
        
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                matchCount++;
                // Điểm cao hơn cho exact match
                if (message.equals(pattern)) {
                    score += 2.0;
                } else if (message.startsWith(pattern) || message.endsWith(pattern)) {
                    score += 1.5;
                } else {
                    score += 1.0;
                }
            }
        }
        
        // Bonus cho nhiều keyword match
        if (matchCount > 1) {
            score *= (1.0 + matchCount * 0.1);
        }
        
        return score;
    }
    
    // Kiểm tra có từ khóa sản phẩm không
    private boolean containsProductKeywords(String message) {
        List<String> productKeywords = Arrays.asList(
            "trà", "cà phê", "coffee", "sữa", "đào", "vải", "chanh", "matcha",
            "latte", "americano", "cappuccino", "espresso", "trân châu", "pudding"
        );
        return productKeywords.stream().anyMatch(message::contains);
    }
    
    // Thực thi ý định
    private ChatResponse executeIntent(IntentScore intent, String message, Long userId) {
        try {
            switch (intent.getIntent()) {
                case "GREETING": return handleGreeting();
                case "GOODBYE": return handleGoodbye();
                case "THANKS": return handleThanks();
                case "MOOD_ASK": return handleMoodAsk();
                case "MOOD_TIRED": return handleMoodTired();
                case "MOOD_HAPPY": return handleMoodHappy();
                case "MOOD_SAD": return handleMoodSad();
                case "MOOD_HOT": return handleMoodHot();
                case "MOOD_COLD": return handleMoodCold();
                case "MOOD_ENERGY": return handleMoodEnergy();
                case "MOOD_RELAX": return handleMoodRelax();
                case "WEATHER_RECOMMEND": return handleWeatherRecommendation();
                case "SIZE_INFO": return handleSizeInfo();
                case "TOPPING_INFO": return handleToppingInfo();
                case "HELP": return handleHelp();
                case "HOURS": return handleOpeningHours();
                case "PAYMENT": return handlePaymentInfo();
                case "DELIVERY": return handleDeliveryInfo();
                case "VOUCHER": return handleVoucherQuery();
                case "STORE": return handleStoreQuery();
                case "ORDER": return handleOrderQuery(userId);
                case "RECOMMEND": return handleRecommendation(message);
                case "CATEGORY": return handleCategoryQuery();
                case "PRICE": return handlePriceQuery(message);
                case "TEA_SEARCH": return handleDrinkSearch(message, "trà");
                case "COFFEE_SEARCH": return handleDrinkSearch(message, "cà phê");
                case "DRINK_SEARCH": return handleDrinkSearch(message, null);
                case "APP": return handleAppInfo();
                case "COMPLAINT": return handleComplaint();
                default: return handleSmartSearch(message);
            }
        } catch (Exception e) {
            log.error("Error executing intent: " + intent.getIntent(), e);
            return handleError();
        }
    }
    
    // Inner class cho intent scoring
    private static class IntentScore {
        private final String intent;
        private final double baseScore;
        private final double weight;
        
        public IntentScore(String intent, double baseScore, double weight) {
            this.intent = intent;
            this.baseScore = baseScore;
            this.weight = weight;
        }
        
        public String getIntent() { return intent; }
        public double getFinalScore() { return baseScore * weight; }
    }

    // ==================== ENHANCED HANDLERS ====================
    
    private ChatResponse handleEmptyMessage() {
        return ChatResponse.builder()
            .message("🤔 Bạn có muốn hỏi gì không?\n\n" +
                "Tôi có thể giúp bạn:\n" +
                "• Tìm đồ uống: \"trà sữa\", \"cà phê\"\n" +
                "• Xem giá: \"giá trà đào\"\n" +
                "• Khuyến mãi: \"voucher\"\n" +
                "• Gợi ý: \"món ngon\"\n\n" +
                "Hãy thử hỏi tôi nhé! 😊")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleTooLongMessage() {
        return ChatResponse.builder()
            .message("😅 Tin nhắn hơi dài rồi bạn ơi!\n\n" +
                "Bạn có thể hỏi ngắn gọn hơn không?\n" +
                "Ví dụ: \"tìm trà sữa\" thay vì câu dài 😊")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleError() {
        return ChatResponse.builder()
            .message("😔 Xin lỗi, có lỗi xảy ra!\n\n" +
                "Bạn thử hỏi lại hoặc liên hệ:\n" +
                "📞 Hotline: 1900-xxxx\n" +
                "💬 Fanpage: fb.com/utetea")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleGreeting() {
        String timeGreeting = getTimeBasedGreeting();
        WeatherDto weather = getWeatherData();
        
        StringBuilder sb = new StringBuilder();
        sb.append(timeGreeting).append("! 👋 Tôi là trợ lý ảo của UTE Tea.\n\n");
        
        // Thêm thông tin thời tiết nếu có
        if (weather != null) {
            sb.append("🌤️ Hôm nay tại ").append(weather.getCity()).append(": ");
            sb.append(String.format("%.0f°C", weather.getTemperature()));
            if (weather.getDescription() != null) {
                sb.append(" - ").append(weather.getDescription());
            }
            sb.append("\n\n");
        }
        
        sb.append("Tôi có thể giúp bạn:\n");
        sb.append("🍵 Tìm kiếm đồ uống yêu thích\n");
        sb.append("💰 Xem giá và khuyến mãi\n");
        sb.append("📍 Tìm cửa hàng gần bạn\n");
        sb.append("🌤️ Gợi ý theo thời tiết\n\n");
        sb.append("Bạn muốn tôi giúp gì nào? 😊");
        
        return ChatResponse.builder()
            .message(sb.toString())
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
                "🌤️ **Gợi ý theo thời tiết:**\n" +
                "• \"Hôm nay uống gì?\" → Gợi ý theo thời tiết\n" +
                "• \"Trời nóng quá\" → Đồ mát lạnh\n" +
                "• \"Trời mưa\" → Đồ ấm nóng\n\n" +
                "😊 **Gợi ý theo tâm trạng:**\n" +
                "• \"Hôm nay mệt quá\" → Đồ uống nhẹ nhàng\n" +
                "• \"Đang vui\" → Món ngọt ngào\n\n" +
                "📏 **Thông tin khác:**\n" +
                "• \"Size\" → Xem các size\n" +
                "• \"Topping\" → Xem topping\n\n" +
                "Hãy thử hỏi tôi bất cứ điều gì! 😊")
            .type("TEXT")
            .build();
    }
    
    // ==================== MOOD HANDLERS ====================
    
    private ChatResponse handleMoodAsk() {
        return ChatResponse.builder()
            .message("💭 **Hôm nay bạn cảm thấy thế nào?**\n\n" +
                "Hãy cho tôi biết tâm trạng của bạn, tôi sẽ gợi ý đồ uống phù hợp nhé!\n\n" +
                "😴 **Mệt mỏi** → Trà xanh, ít đường\n" +
                "😊 **Vui vẻ** → Trà sữa topping đầy đủ\n" +
                "😢 **Buồn** → Đồ ngọt an ủi\n" +
                "🥵 **Nóng** → Đá xay, sinh tố mát lạnh\n" +
                "🥶 **Lạnh** → Đồ nóng ấm áp\n" +
                "⚡ **Cần năng lượng** → Cà phê đậm đà\n" +
                "😌 **Thư giãn** → Trà hoa, trà thảo mộc\n\n" +
                "Gõ tâm trạng của bạn nhé! 💚")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleMoodTired() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Lọc đồ uống phù hợp: trà xanh, trà thảo mộc, ít đường
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("trà xanh") || name.contains("trà hoa") || 
                       name.contains("matcha") || name.contains("thảo mộc") ||
                       name.contains("detox") || name.contains("green tea");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        // Nếu không có, lấy random
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("😴 **Tôi hiểu bạn đang mệt...**\n\n");
        sb.append("Khi mệt mỏi, bạn nên uống đồ nhẹ nhàng, ít đường để cơ thể dễ hấp thu!\n\n");
        sb.append("🍵 **Gợi ý cho bạn:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Chọn size nhỏ, ít đường hoặc không đường nhé!\n");
        sb.append("Nghỉ ngơi nhiều và uống đủ nước nha! 💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodHappy() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Lọc đồ uống vui vẻ: trà sữa, topping nhiều, vị ngọt
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("trà sữa") || name.contains("milk tea") || 
                       name.contains("trân châu") || name.contains("topping") ||
                       name.contains("cheese") || name.contains("kem");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 **Tuyệt vời! Bạn đang vui!**\n\n");
        sb.append("Hãy thưởng cho mình một ly đồ uống ngon lành nhé!\n\n");
        sb.append("🧋 **Gợi ý cho ngày vui:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Thêm topping trân châu, pudding cho đã! 🎊\n");
        sb.append("Chúc bạn luôn vui vẻ! 💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodSad() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Đồ ngọt an ủi
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("chocolate") || name.contains("socola") || 
                       name.contains("caramel") || name.contains("kem") ||
                       name.contains("sữa") || name.contains("matcha");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("🤗 **Ôi, bạn đang buồn à?**\n\n");
        sb.append("Đừng lo, một ly đồ uống ngọt ngào sẽ giúp bạn cảm thấy tốt hơn!\n\n");
        sb.append("🍫 **Gợi ý an ủi:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Chocolate và đồ ngọt giúp tăng endorphin - hormone hạnh phúc!\n");
        sb.append("Mọi chuyện rồi sẽ ổn thôi! 💚🌈");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodHot() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Đồ mát lạnh
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("đá") || name.contains("ice") || 
                       name.contains("sinh tố") || name.contains("smoothie") ||
                       name.contains("freeze") || name.contains("lạnh") ||
                       name.contains("chanh") || name.contains("dừa");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("🥵 **Trời nóng quá phải không!**\n\n");
        sb.append("Để tôi gợi ý những món mát lạnh giải nhiệt cho bạn!\n\n");
        sb.append("🧊 **Đồ uống giải nhiệt:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Chọn size lớn, thêm đá để mát hơn!\n");
        sb.append("Giữ mát và uống đủ nước nhé! 🧊💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodCold() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Đồ nóng ấm
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("nóng") || name.contains("hot") || 
                       name.contains("ấm") || name.contains("warm") ||
                       name.contains("cà phê") || name.contains("coffee") ||
                       name.contains("trà") && !name.contains("đá");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("🥶 **Trời lạnh rồi nhỉ!**\n\n");
        sb.append("Một ly đồ uống nóng sẽ giúp bạn ấm áp hơn!\n\n");
        sb.append("☕ **Đồ uống ấm áp:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Chọn đồ nóng, thêm gừng nếu có!\n");
        sb.append("Giữ ấm và khỏe mạnh nhé! ☕💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodEnergy() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Cà phê, đồ có caffeine
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("cà phê") || name.contains("coffee") || 
                       name.contains("cafe") || name.contains("espresso") ||
                       name.contains("latte") || name.contains("americano") ||
                       name.contains("matcha") || name.contains("trà đen");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("⚡ **Cần năng lượng để chiến đấu!**\n\n");
        sb.append("Caffeine sẽ giúp bạn tỉnh táo và tập trung hơn!\n\n");
        sb.append("☕ **Đồ uống tăng năng lượng:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Cà phê đen ít đường giúp tỉnh táo nhanh nhất!\n");
        sb.append("Chúc bạn làm việc/học tập hiệu quả! ⚡💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private ChatResponse handleMoodRelax() {
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        // Trà hoa, thảo mộc, nhẹ nhàng
        List<Drink> recommended = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                return name.contains("trà hoa") || name.contains("hoa") || 
                       name.contains("thảo mộc") || name.contains("herbal") ||
                       name.contains("trà xanh") || name.contains("oolong") ||
                       name.contains("sen") || name.contains("nhài");
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (recommended.isEmpty()) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("😌 **Thư giãn là điều tuyệt vời!**\n\n");
        sb.append("Một ly trà nhẹ nhàng sẽ giúp bạn thư thái hơn!\n\n");
        sb.append("🍃 **Đồ uống thư giãn:**\n\n");
        
        for (Drink d : recommended) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 **Tip:** Trà hoa và thảo mộc giúp giảm stress hiệu quả!\n");
        sb.append("Tận hưởng khoảnh khắc bình yên nhé! 🍃💚");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    // ==================== WEATHER-BASED RECOMMENDATION ====================
    
    private WeatherDto getWeatherData() {
        long now = System.currentTimeMillis();
        if (cachedWeather == null || (now - weatherCacheTime) > WEATHER_CACHE_DURATION) {
            try {
                cachedWeather = weatherService.getCurrentWeather();
                weatherCacheTime = now;
            } catch (Exception e) {
                log.warn("Could not fetch weather data: {}", e.getMessage());
                // Trả về null nếu không lấy được thời tiết
                return null;
            }
        }
        return cachedWeather;
    }
    
    private ChatResponse handleWeatherRecommendation() {
        WeatherDto weather = getWeatherData();
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        if (drinks.isEmpty()) {
            return ChatResponse.builder()
                .message("😅 Xin lỗi, hiện tại chưa có đồ uống nào.")
                .type("TEXT")
                .build();
        }
        
        List<Drink> recommended;
        StringBuilder sb = new StringBuilder();
        
        if (weather != null) {
            double temp = weather.getTemperature();
            String condition = weather.getCondition();
            boolean isRainy = condition != null && 
                (condition.equalsIgnoreCase("Rain") || 
                 condition.equalsIgnoreCase("Drizzle") ||
                 condition.equalsIgnoreCase("Thunderstorm"));
            
            sb.append("🌤️ **Gợi ý theo thời tiết hôm nay**\n\n");
            sb.append("📍 ").append(weather.getCity()).append(" - ");
            sb.append(String.format("%.1f°C", temp));
            if (weather.getDescription() != null) {
                sb.append(" (").append(weather.getDescription()).append(")");
            }
            sb.append("\n\n");
            
            if (isRainy) {
                sb.append("🌧️ **Trời đang mưa!**\n");
                sb.append("Một ly đồ uống ấm sẽ rất tuyệt!\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("nóng", "hot", "ấm", "cacao", "gừng", "cà phê"));
            } else if (temp >= 32) {
                sb.append("🥵 **Trời nóng quá!**\n");
                sb.append("Đồ uống mát lạnh là lựa chọn hoàn hảo!\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("đá", "ice", "lạnh", "freeze", "sinh tố", "smoothie", "chanh", "dừa"));
            } else if (temp >= 28) {
                sb.append("☀️ **Thời tiết ấm áp!**\n");
                sb.append("Đây là những món phù hợp cho bạn:\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("trà sữa", "milk tea", "trà đào", "trà vải", "matcha"));
            } else if (temp < 22) {
                sb.append("🥶 **Trời se lạnh!**\n");
                sb.append("Đồ uống nóng sẽ giúp bạn ấm áp hơn!\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("nóng", "hot", "ấm", "cà phê", "cacao", "gừng"));
            } else {
                sb.append("🌤️ **Thời tiết dễ chịu!**\n");
                sb.append("Bạn có thể thưởng thức bất kỳ món nào!\n\n");
                Collections.shuffle(drinks);
                recommended = drinks.stream().limit(5).collect(Collectors.toList());
            }
        } else {
            // Fallback khi không có dữ liệu thời tiết - gợi ý theo thời gian
            int hour = LocalTime.now().getHour();
            sb.append("☕ **Gợi ý theo thời điểm**\n\n");
            
            if (hour < 10) {
                sb.append("🌅 **Buổi sáng tươi mới!**\n");
                sb.append("Một ly cà phê hoặc trà để bắt đầu ngày mới:\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("cà phê", "coffee", "trà xanh", "matcha"));
            } else if (hour < 14) {
                sb.append("🌞 **Giữa trưa!**\n");
                sb.append("Đồ uống mát lạnh giải nhiệt:\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("đá", "lạnh", "sinh tố", "trà đào", "trà vải"));
            } else if (hour < 18) {
                sb.append("🌤️ **Buổi chiều!**\n");
                sb.append("Thời điểm hoàn hảo cho trà sữa:\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("trà sữa", "milk tea", "trân châu"));
            } else {
                sb.append("🌙 **Buổi tối!**\n");
                sb.append("Đồ uống nhẹ nhàng thư giãn:\n\n");
                recommended = filterDrinksByKeywords(drinks, 
                    Arrays.asList("trà", "thảo mộc", "ít đường", "matcha"));
            }
        }
        
        // Đảm bảo có ít nhất 5 món
        if (recommended.isEmpty() || recommended.size() < 3) {
            Collections.shuffle(drinks);
            recommended = drinks.stream().limit(5).collect(Collectors.toList());
        }
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .limit(5)
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        sb.append("🧋 **Đồ uống gợi ý:**\n\n");
        for (Drink d : recommended.stream().limit(5).collect(Collectors.toList())) {
            sb.append("• **").append(d.getName()).append("** - ")
              .append(formatPrice(d.getBasePrice())).append("\n");
        }
        
        sb.append("\n💡 Gõ tên món để xem chi tiết và đặt hàng!");
        
        return ChatResponse.builder()
            .message(sb.toString())
            .type("DRINKS")
            .data(drinkDtos)
            .build();
    }
    
    private List<Drink> filterDrinksByKeywords(List<Drink> drinks, List<String> keywords) {
        List<Drink> filtered = drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                String desc = d.getDescription() != null ? d.getDescription().toLowerCase() : "";
                return keywords.stream().anyMatch(k -> name.contains(k) || desc.contains(k));
            })
            .collect(Collectors.toList());
        
        Collections.shuffle(filtered);
        return filtered.stream().limit(5).collect(Collectors.toList());
    }
    
    private ChatResponse handleSizeInfo() {
        return ChatResponse.builder()
            .message("📏 **Thông tin Size đồ uống**\n\n" +
                "UTE Tea có 3 size cho bạn lựa chọn:\n\n" +
                "🥤 **Size S (Nhỏ)**\n" +
                "   • Dung tích: ~350ml\n" +
                "   • Phù hợp: Uống nhẹ, thử vị mới\n\n" +
                "🥤 **Size M (Vừa)** ⭐ Phổ biến nhất\n" +
                "   • Dung tích: ~500ml\n" +
                "   • Phù hợp: Đa số khách hàng\n\n" +
                "🥤 **Size L (Lớn)**\n" +
                "   • Dung tích: ~700ml\n" +
                "   • Phù hợp: Uống nhiều, chia sẻ\n\n" +
                "💡 **Tip:** Size M là lựa chọn cân bằng nhất!\n" +
                "Giá sẽ tăng thêm 5.000đ - 10.000đ cho mỗi size lớn hơn.")
            .type("TEXT")
            .build();
    }
    
    private ChatResponse handleToppingInfo() {
        return ChatResponse.builder()
            .message("🧋 **Topping UTE Tea**\n\n" +
                "Thêm topping để đồ uống ngon hơn!\n\n" +
                "⚫ **Trân châu đen** - 8.000đ\n" +
                "   Dai giòn, vị đường đen\n\n" +
                "⚪ **Trân châu trắng** - 8.000đ\n" +
                "   Mềm dẻo, vị sữa\n\n" +
                "🟤 **Pudding** - 10.000đ\n" +
                "   Mềm mịn, béo ngậy\n\n" +
                "🟢 **Thạch** - 8.000đ\n" +
                "   Giòn mát, nhiều vị\n\n" +
                "🧀 **Cheese foam** - 12.000đ\n" +
                "   Béo mặn, trending!\n\n" +
                "🖤 **Đường đen** - 5.000đ\n" +
                "   Ngọt tự nhiên\n\n" +
                "🟫 **Sương sáo** - 8.000đ\n" +
                "   Mát lành, giải nhiệt\n\n" +
                "💡 **Tip:** Combo trân châu + pudding rất được yêu thích!")
            .type("TEXT")
            .build();
    }
    
    // ==================== OTHER HANDLERS ====================
    
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
        
        if (drinks.isEmpty()) {
            return ChatResponse.builder()
                .message("😅 Xin lỗi, hiện tại chưa có đồ uống nào.")
                .type("TEXT")
                .build();
        }
        
        // Lọc theo context nếu có
        String context = extractContext(message);
        List<Drink> filteredDrinks = drinks;
        
        if (context != null) {
            final String ctx = context;
            filteredDrinks = drinks.stream()
                .filter(d -> d.getName().toLowerCase().contains(ctx) ||
                            (d.getCategory() != null && 
                             d.getCategory().getName().toLowerCase().contains(ctx)))
                .collect(Collectors.toList());
        }
        
        // Nếu không có filter hoặc filter rỗng, thử gợi ý theo thời tiết
        if (filteredDrinks.isEmpty() || context == null) {
            WeatherDto weather = getWeatherData();
            if (weather != null) {
                double temp = weather.getTemperature();
                String condition = weather.getCondition();
                boolean isRainy = condition != null && 
                    (condition.equalsIgnoreCase("Rain") || 
                     condition.equalsIgnoreCase("Drizzle") ||
                     condition.equalsIgnoreCase("Thunderstorm"));
                
                List<String> keywords;
                if (isRainy) {
                    keywords = Arrays.asList("nóng", "hot", "ấm", "cacao", "gừng");
                } else if (temp >= 32) {
                    keywords = Arrays.asList("đá", "ice", "lạnh", "freeze", "sinh tố", "chanh");
                } else if (temp >= 28) {
                    keywords = Arrays.asList("trà sữa", "milk tea", "trà đào", "matcha");
                } else if (temp < 22) {
                    keywords = Arrays.asList("nóng", "hot", "ấm", "cà phê");
                } else {
                    keywords = Arrays.asList("trà sữa", "trà đào", "cà phê");
                }
                
                filteredDrinks = filterDrinksByKeywords(drinks, keywords);
            }
        }
        
        // Fallback: random từ tất cả
        if (filteredDrinks.isEmpty()) {
            filteredDrinks = new ArrayList<>(drinks);
        }
        
        // Random 5 món làm "best seller"
        Collections.shuffle(filteredDrinks);
        List<Drink> recommended = filteredDrinks.stream().limit(5).collect(Collectors.toList());
        
        List<DrinkDto> drinkDtos = recommended.stream()
            .map(drinkMapper::toDto)
            .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        
        // Thêm thông tin thời tiết nếu có
        WeatherDto weather = getWeatherData();
        if (weather != null) {
            sb.append("🔥 **Món hot hôm nay!**\n");
            sb.append("📍 ").append(weather.getCity()).append(" - ");
            sb.append(String.format("%.0f°C", weather.getTemperature()));
            if (weather.getDescription() != null) {
                sb.append(" (").append(weather.getDescription()).append(")");
            }
            sb.append("\n\n");
        } else {
            sb.append("🔥 **Món hot được yêu thích!**\n\n");
        }
        
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
        
        // 🤖 Sử dụng Gemini AI để trả lời thông minh
        if (USE_GEMINI_AI) {
            try {
                // Lấy thông tin context
                String weatherContext = "";
                WeatherDto weather = getWeatherData();
                if (weather != null) {
                    String desc = weather.getDescription() != null ? weather.getDescription() : "Bình thường";
                    weatherContext = String.format("Thời tiết: %s, %.0f°C", desc, weather.getTemperature());
                }
                
                // Lấy danh sách món phổ biến để AI biết
                List<Drink> popularDrinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory()
                    .stream().limit(10).collect(Collectors.toList());
                StringBuilder menuContext = new StringBuilder("Các món phổ biến: ");
                for (Drink d : popularDrinks) {
                    menuContext.append(d.getName()).append(" (").append(formatPrice(d.getBasePrice())).append("), ");
                }
                
                String context = weatherContext + "\n" + menuContext.toString();
                String aiResponse = geminiService.generateResponse(message, context);
                
                if (aiResponse != null && !aiResponse.isEmpty()) {
                    return ChatResponse.builder()
                        .message("🤖 " + aiResponse)
                        .type("TEXT")
                        .build();
                }
            } catch (Exception e) {
                log.warn("Gemini AI fallback failed: {}", e.getMessage());
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
