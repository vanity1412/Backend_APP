package com.utetea.backend.service;

import com.utetea.backend.model.Notification;
import com.utetea.backend.model.NotificationType;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.NotificationRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
public class OneSignalService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Value("${onesignal.rest.api.key}")
    private String restApiKey;

    @Value("${onesignal.app.id}")
    private String appId;

    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";

    public OneSignalService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Hàm gửi request cơ bản
    private void sendRequest(String jsonBody) {
        try {
            log.info("OneSignal Request Body: {}", jsonBody);
            
            URL url = new URL(ONESIGNAL_API_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setDoInput(true);

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Authorization", "Basic " + restApiKey);

            byte[] sendBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(sendBytes.length);

            try (OutputStream outputStream = con.getOutputStream()) {
                outputStream.write(sendBytes);
            }

            int httpResponse = con.getResponseCode();
            
            // Đọc response body để debug
            String responseBody = "";
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            httpResponse >= 400 ? con.getErrorStream() : con.getInputStream(), 
                            StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                responseBody = sb.toString();
            }
            
            if (httpResponse >= 200 && httpResponse < 300) {
                log.info("OneSignal Response: {} - {}", httpResponse, responseBody);
            } else {
                log.error("OneSignal Error: {} - {}", httpResponse, responseBody);
            }

        } catch (Throwable t) {
            log.error("Error sending OneSignal notification", t);
        }
    }

    // 1. Gửi cho TẤT CẢ User
    public void sendToAll(String title, String content) {
        sendToAll(title, content, NotificationType.CUSTOM, null);
    }

    // 1b. Gửi cho TẤT CẢ User với type và relatedId
    @Transactional
    public void sendToAll(String title, String content, NotificationType type, Long relatedId) {
        String jsonBody = "{"
                + "\"app_id\": \"" + appId + "\","
                + "\"included_segments\": [\"All\"],"
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);

        // Lưu thông báo cho tất cả user
        try {
            userRepository.findAll().forEach(user -> {
                saveNotificationToDb(user, title, content, type, relatedId);
            });
        } catch (Exception e) {
            log.error("Failed to save notifications to database", e);
        }
    }

    // 2. Gửi cho User cụ thể (theo ID Database)
    public void sendToUser(String userId, String title, String content) {
        sendToUser(userId, title, content, NotificationType.SYSTEM, null);
    }

    // 2b. Gửi cho User cụ thể với type và relatedId
    @Transactional
    public void sendToUser(String userId, String title, String content, NotificationType type, Long relatedId) {
        String jsonBody = "{"
                + "\"app_id\": \"" + appId + "\","
                + "\"include_aliases\": {\"external_id\": [\"" + userId + "\"]},"
                + "\"target_channel\": \"push\","
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);

        // Lưu thông báo vào database
        try {
            Long userIdLong = Long.parseLong(userId);
            Optional<User> userOpt = userRepository.findById(userIdLong);
            userOpt.ifPresent(user -> saveNotificationToDb(user, title, content, type, relatedId));
        } catch (Exception e) {
            log.error("Failed to save notification to database for user {}", userId, e);
        }
    }

    // 3. Gửi cho danh sách nhiều User
    public void sendToMultipleUsers(String[] userIds, String title, String content) {
        sendToMultipleUsers(userIds, title, content, NotificationType.CUSTOM, null);
    }

    // 3b. Gửi cho danh sách nhiều User với type và relatedId
    @Transactional
    public void sendToMultipleUsers(String[] userIds, String title, String content, NotificationType type, Long relatedId) {
        StringBuilder idsJson = new StringBuilder("[");
        for (int i = 0; i < userIds.length; i++) {
            idsJson.append("\"").append(userIds[i]).append("\"");
            if (i < userIds.length - 1) idsJson.append(",");
        }
        idsJson.append("]");

        String jsonBody = "{"
                + "\"app_id\": \"" + appId + "\","
                + "\"include_aliases\": {\"external_id\": " + idsJson.toString() + "},"
                + "\"target_channel\": \"push\","
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);

        // Lưu thông báo vào database cho từng user
        try {
            for (String userId : userIds) {
                Long userIdLong = Long.parseLong(userId);
                Optional<User> userOpt = userRepository.findById(userIdLong);
                userOpt.ifPresent(user -> saveNotificationToDb(user, title, content, type, relatedId));
            }
        } catch (Exception e) {
            log.error("Failed to save notifications to database", e);
        }
    }

    /**
     * Lưu thông báo vào database
     * KHÔNG lưu cho GROUP_CHAT và LIVE_CHAT (chỉ gửi push notification realtime)
     */
    private void saveNotificationToDb(User user, String title, String content, NotificationType type, Long relatedId) {
        // Không lưu notification cho Group Chat và Live Chat
        if (type == NotificationType.GROUP_CHAT || type == NotificationType.LIVE_CHAT) {
            log.debug("Skipping DB save for {} notification (realtime only)", type);
            return;
        }
        
        try {
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .content(content)
                    .type(type)
                    .relatedId(relatedId)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
            log.debug("Saved notification to DB for user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to save notification for user {}", user.getId(), e);
        }
    }
}