package com.utetea.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class DrinkImageUploadService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    /**
     * Upload ảnh drink lên GitHub vào folder assets/drinks
     * @param file File ảnh cần upload
     * @param drinkName Tên drink để đặt tên file
     * @return URL của ảnh đã upload
     */
    public String uploadDrinkImage(MultipartFile file, String drinkName) throws IOException {
        // 1. Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        
        // Sanitize drink name để làm tên file
        String sanitizedName = drinkName.toLowerCase()
                .replaceAll("[^a-z0-9\\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
        
        if (sanitizedName.length() > 30) {
            sanitizedName = sanitizedName.substring(0, 30);
        }
        
        String fileName = "drink_" + sanitizedName + "_" + System.currentTimeMillis() + extension;

        // 2. Đường dẫn API GitHub - upload vào folder drinks
        String apiUrl = "https://api.github.com/repos/xinloihuy/git_test/contents/assets/drinks/" + fileName;

        // 3. Chuẩn bị Body request (JSON)
        Map<String, String> body = new HashMap<>();
        body.put("message", "Upload drink image: " + drinkName);
        body.put("content", Base64.getEncoder().encodeToString(file.getBytes()));
        body.put("branch", "main");

        // 4. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        // 5. Gọi API GitHub
        try {
            restTemplate.exchange(apiUrl, HttpMethod.PUT, requestEntity, Map.class);

            // 6. Trả về link Raw
            return String.format("https://raw.githubusercontent.com/xinloihuy/git_test/main/assets/drinks/%s", fileName);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload ảnh drink lên GitHub: " + e.getMessage());
        }
    }
}
