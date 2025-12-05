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
import java.util.UUID;

@Service
public class AvatarUploadService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    public String uploadFile(MultipartFile file, String userId) throws IOException {
        // 1. Tạo tên file duy nhất để tránh cache và trùng lặp
        // Ví dụ: user_1_1709898.jpg
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String fileName = "user_" + userId + "_" + System.currentTimeMillis() + extension;

        // 2. Đường dẫn API GitHub
        String apiUrl = "https://api.github.com/repos/vanity1412/UTEtea-img/contents/main/" + fileName;

        // 3. Chuẩn bị Body request (JSON)
        Map<String, String> body = new HashMap<>();
        body.put("message", "Update avatar for user " + userId);
        // GitHub yêu cầu content là Base64
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

            // 6. Trả về link Raw để lưu vào DB
            // Link dạng: https://raw.githubusercontent.com/USER/REPO/BRANCH/PATH/FILENAME
            return String.format("https://raw.githubusercontent.com/vanity1412/UTEtea-img/main/assets/users/avatar/%s", fileName);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload ảnh lên GitHub: " + e.getMessage());
        }
    }
}