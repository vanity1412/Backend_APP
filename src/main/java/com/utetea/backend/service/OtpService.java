package com.utetea.backend.service;

import com.utetea.backend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final JavaMailSender mailSender;
    private final com.utetea.backend.repository.UserRepository userRepository;
    
    private static final long OTP_VALIDITY_MINUTES = 5; // OTP valid for 5 minutes
    
    /**
     * Generate 6-digit OTP
     */
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(otp);
    }
    
    /**
     * Send OTP to phone (via email for now, can integrate SMS later)
     */
    public void sendOtp(String otp, String email) {
        try {
        } catch (Exception e) {
            System.err.println("ERROR saving user with OTP: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // Send OTP via email
        System.out.println("Preparing to send email...");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("watershoputetea@gmail.com");
            message.setTo(email);
            message.setSubject("UTE Tea - Mã OTP xác thực tài khoản");
            message.setText(
                    "Xin chào,\n\n" +
                            "Mã OTP của bạn là: " + otp + "\n\n" +
                            "Mã này có hiệu lực trong " + OTP_VALIDITY_MINUTES + " phút.\n\n" +
                            "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.\n\n" +
                            "Trân trọng,\n" +
                            "UTE Tea Team"
            );

            System.out.println("Sending email to: " + email);
            mailSender.send(message);
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            System.err.println("ERROR sending email: " + e.getMessage());
            e.printStackTrace();
            log.error("Failed to send OTP email: {}", e.getMessage());
            throw new RuntimeException("Failed to send OTP email");
        }

        System.out.println("========== OtpService.sendOtp() END ==========");
    }
    
    /**
     * Verify OTP
     */
    public boolean verifyOtp(String phone, String otp) {
        com.utetea.backend.model.User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            log.warn("User not found for phone: {}", phone);
            return false;
        }
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            log.warn("No OTP stored for user: {}", user.getUsername());
            return false;
        }
        if (java.time.LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            log.warn("OTP expired for user: {}", user.getUsername());
            clearOtp(phone);
            return false;
        }
        boolean isValid = user.getOtp().equals(otp);
        if (isValid) {
            clearOtp(phone);
            log.info("OTP verified successfully for user: {}", user.getUsername());
        } else {
            log.warn("Invalid OTP for user: {}", user.getUsername());
        }
        return isValid;
    }

    public boolean verifyOtpByEmail(String email, String otp) {
        com.utetea.backend.model.User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("User not found for email: {}", email);
            return false;
        }
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            log.warn("No OTP stored for user: {}", user.getUsername());
            return false;
        }
        if (java.time.LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            log.warn("OTP expired for user: {}", user.getUsername());
            clearOtpByEmail(email);
            return false;
        }
        boolean isValid = user.getOtp().equals(otp);
        if (isValid) {
            clearOtpByEmail(email);
            log.info("OTP verified successfully for user: {}", user.getUsername());
        } else {
            log.warn("Invalid OTP for user: {}", user.getUsername());
        }
        return isValid;
    }
    
    /**
     * Clear OTP for a phone number
     */
    public void clearOtp(String phone) {
        userRepository.findByPhone(phone).ifPresent(u -> {
            u.setOtp(null);
            u.setOtpExpiry(null);
            userRepository.save(u);
        });
    }

    public void clearOtpByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setOtp(null);
            u.setOtpExpiry(null);
            userRepository.save(u);
        });
    }
}
