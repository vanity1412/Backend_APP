package com.utetea.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class OneSignalService {

    private static final String REST_API_KEY = "os_v2_app_4r5fs33yt5cxhmtqntiq6vl72otperklfqzu6znmtwjnihlfpmus44fdlftut3jzboc3oy7n3yjisfgt4afi2ll4rb2v5k42gofpp2a";
    private static final String APP_ID = "e47a596f-789f-4573-b270-6cd10f557fd3";
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";

    // Hàm gửi request cơ bản
    private void sendRequest(String jsonBody) {
        try {
            URL url = new URL(ONESIGNAL_API_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setDoInput(true);

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Authorization", "Basic " + REST_API_KEY);

            byte[] sendBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(sendBytes.length);

            try (OutputStream outputStream = con.getOutputStream()) {
                outputStream.write(sendBytes);
            }

            int httpResponse = con.getResponseCode();
            log.info("OneSignal Response Code: {}", httpResponse);

        } catch (Throwable t) {
            log.error("Error sending OneSignal notification", t);
        }
    }

    // 1. Gửi cho TẤT CẢ User
    public void sendToAll(String title, String content) {
        String jsonBody = "{"
                + "\"app_id\": \"" + APP_ID + "\","
                + "\"included_segments\": [\"All\"],"
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);
    }

    // 2. Gửi cho User cụ thể (theo ID Database)
    public void sendToUser(String userId, String title, String content) {
        String jsonBody = "{"
                + "\"app_id\": \"" + APP_ID + "\","
                + "\"include_aliases\": {\"external_id\": [\"" + userId + "\"]},"
                + "\"target_channel\": \"push\","
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);
    }

    // 3. Gửi cho danh sách nhiều User
    public void sendToMultipleUsers(String[] userIds, String title, String content) {
        StringBuilder idsJson = new StringBuilder("[");
        for (int i = 0; i < userIds.length; i++) {
            idsJson.append("\"").append(userIds[i]).append("\"");
            if (i < userIds.length - 1) idsJson.append(",");
        }
        idsJson.append("]");

        String jsonBody = "{"
                + "\"app_id\": \"" + APP_ID + "\","
                + "\"include_aliases\": {\"external_id\": " + idsJson.toString() + "},"
                + "\"target_channel\": \"push\","
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + content + "\"}"
                + "}";
        sendRequest(jsonBody);
    }
}