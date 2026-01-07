package com.utetea.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key:AIzaSyAKPLUXfXedp03NziJDHebqV_SIbD-0oUQ}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash-preview-05-20}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // System prompt cho chatbot UTE Tea
    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý ảo thông minh của UTE Tea - một chuỗi cửa hàng trà sữa và cà phê.
        
        THÔNG TIN VỀ UTE TEA:
        - Chuyên bán trà sữa, cà phê, sinh tố, nước ép
        - Có nhiều chi nhánh tại Việt Nam
        - Giờ mở cửa: 7:00 - 22:00 hàng ngày
        - Hỗ trợ giao hàng và đặt hàng qua app
        - Có chương trình tích điểm, voucher khuyến mãi
        
        CÁCH TRẢ LỜI:
        - Trả lời bằng tiếng Việt, thân thiện, vui vẻ
        - Sử dụng emoji phù hợp để tạo cảm giác gần gũi
        - Trả lời ngắn gọn, súc tích (tối đa 200 từ)
        - Nếu được hỏi về đồ uống cụ thể, gợi ý các món phù hợp
        - Nếu không biết thông tin chính xác, hướng dẫn liên hệ hotline
        - Luôn kết thúc bằng câu hỏi hoặc gợi ý để tiếp tục hội thoại
        
        LƯU Ý:
        - Không trả lời các câu hỏi không liên quan đến đồ uống, cửa hàng
        - Không đưa ra thông tin sai lệch về giá cả (hướng dẫn xem menu)
        - Giữ thái độ tích cực, chuyên nghiệp
        """;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gọi Gemini API để tạo response thông minh
     */
    public String generateResponse(String userMessage, String context) {
        try {
            String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
            );

            // Tạo prompt với context
            String fullPrompt = buildPrompt(userMessage, context);

            // Tạo request body
            Map<String, Object> requestBody = buildRequestBody(fullPrompt);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Gọi API
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );

            // Parse response
            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return null; // Return null để fallback về logic cũ
        }
    }

    /**
     * Tạo prompt đầy đủ với context
     */
    private String buildPrompt(String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT);
        prompt.append("\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("THÔNG TIN BỔ SUNG:\n");
            prompt.append(context);
            prompt.append("\n\n");
        }
        
        prompt.append("KHÁCH HÀNG HỎI: ");
        prompt.append(userMessage);
        prompt.append("\n\nTRẢ LỜI:");
        
        return prompt.toString();
    }

    /**
     * Tạo request body cho Gemini API
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // Contents
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));
        
        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 500);
        requestBody.put("generationConfig", generationConfig);
        
        // Safety settings
        List<Map<String, String>> safetySettings = List.of(
            Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
        );
        requestBody.put("safetySettings", safetySettings);
        
        return requestBody;
    }

    /**
     * Parse response từ Gemini API
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Kiểm tra có candidates không
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            
            // Kiểm tra error
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                log.error("Gemini API error: {}", error.path("message").asText());
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gợi ý đồ uống thông minh dựa trên context
     */
    public String suggestDrinks(String userMessage, String weatherInfo, String moodInfo) {
        String context = String.format("""
            THỜI TIẾT HIỆN TẠI: %s
            TÂM TRẠNG KHÁCH HÀNG: %s
            
            Hãy gợi ý 3-5 loại đồ uống phù hợp với thời tiết và tâm trạng.
            Giải thích ngắn gọn tại sao món đó phù hợp.
            """, 
            weatherInfo != null ? weatherInfo : "Không rõ",
            moodInfo != null ? moodInfo : "Bình thường"
        );
        
        return generateResponse(userMessage, context);
    }

    /**
     * Trả lời câu hỏi về menu/sản phẩm
     */
    public String answerMenuQuestion(String userMessage, String menuInfo) {
        String context = String.format("""
            THÔNG TIN MENU:
            %s
            
            Dựa vào thông tin menu trên, trả lời câu hỏi của khách hàng.
            Nếu không có thông tin chính xác, hướng dẫn xem menu trong app.
            """, menuInfo);
        
        return generateResponse(userMessage, context);
    }

    /**
     * Xử lý hội thoại tự nhiên
     */
    public String chat(String userMessage) {
        return generateResponse(userMessage, null);
    }
}
