package com.utetea.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HttpSmsService {

    @Value("${httpsms.api.key}")
    private String apiKey;

    @Value("${httpsms.url}")
    private String apiUrl;

    @Value("${httpsms.phone.number}")
    private String senderPhoneNumber; // Số máy Android Gateway (+84...)

    private final RestTemplate restTemplate = new RestTemplate();

    // Bộ nhớ lưu OTP tạm thời
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    /**
     * Gửi OTP
     * @param rawPhone Số điện thoại người dùng nhập (VD: 0332249178)
     */
    public boolean sendOtp(String rawPhone) {
        try {
            // 1. Chuẩn hóa số điện thoại sang định dạng +84...
            String recipientPhone = formatToE164(rawPhone);
            log.info("Đang gửi OTP tới số đã chuẩn hóa: {}", recipientPhone);

            // 2. Tạo OTP ngẫu nhiên 6 số
            String otpCode = String.valueOf((int) ((Math.random() * 900000) + 100000));

            // [QUAN TRỌNG] Log OTP ra console để test nhanh nếu SMS chưa tới kịp
            log.info(">>> OTP TEST CODE CHO {}: {}", recipientPhone, otpCode);

            // 3. Lưu OTP vào bộ nhớ (dùng số đã chuẩn hóa làm Key)
            otpStorage.put(recipientPhone, otpCode);
            otpExpiry.put(recipientPhone, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

            // 4. Chuẩn bị dữ liệu gửi (JSON)
            // Lấy thời gian hiện tại: VD "31/12 10:30:45"
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"));

            // Nội dung tin nhắn sẽ luôn khác nhau nhờ giây thay đổi liên tục
            String messageContent = "Ma OTP Houjicha: " + otpCode + ". Date: " + timeStamp + ". g0P9cZJc428";

            // Chuẩn bị payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", messageContent);
            payload.put("from", senderPhoneNumber);
            payload.put("to", recipientPhone);

            // 5. Tạo Header xác thực
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // 6. Gọi API httpSMS
            String response = restTemplate.postForObject(apiUrl, request, String.class);

            log.info("HttpSMS Response: {}", response);
            return true;

        } catch (Exception e) {
            log.error("HttpSMS Error: ", e);
            return false;
        }
    }

    /**
     * Xác thực OTP
     * @param rawPhone Số điện thoại người dùng nhập (VD: 0332249178)
     * @param inputOtp Mã OTP người dùng nhập
     */
    public boolean verifyOtp(String rawPhone, String inputOtp) {
        // Phải chuẩn hóa số điện thoại giống hệt lúc gửi thì mới tìm thấy trong Map
        String phone = formatToE164(rawPhone);

        if (!otpStorage.containsKey(phone)) {
            return false;
        }

        if (System.currentTimeMillis() > otpExpiry.get(phone)) {
            otpStorage.remove(phone);
            otpExpiry.remove(phone);
            return false; // Hết hạn
        }

        String storedOtp = otpStorage.get(phone);
        if (storedOtp.equals(inputOtp)) {
            // Xác thực thành công -> Xóa OTP
            otpStorage.remove(phone);
            otpExpiry.remove(phone);
            return true;
        }

        return false;
    }

    /**
     * Hàm chuẩn hóa số điện thoại Việt Nam sang chuẩn E.164
     * VD: 0339969176 -> +84339969176
     */
    private String formatToE164(String phone) {
        if (phone == null || phone.isEmpty()) return "";

        // Xóa khoảng trắng hoặc dấu gạch ngang nếu có
        String cleanPhone = phone.replaceAll("\\s+", "").replaceAll("-", "");

        if (cleanPhone.startsWith("0")) {
            return "+84" + cleanPhone.substring(1);
        }
        if (cleanPhone.startsWith("84")) {
            return "+" + cleanPhone;
        }
        if (!cleanPhone.startsWith("+")) {
            // Nếu nhập thiếu cả 0 lẫn +84 (hiếm gặp), mặc định thêm +84
            return cleanPhone;
        }
        return cleanPhone;
    }
}