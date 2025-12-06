package com.utetea.backend.service;

import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    
    private static final long OTP_VALIDITY_MINUTES = 5;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate 6-digit OTP using SecureRandom for better security
     */
    public String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
    
    /**
     * Send OTP via email
     */
    public void sendOtp(String otp, String email) {
        log.info("Preparing to send OTP email to: {}", email);
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

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email");
        }
    }
    
    /**
     * Verify OTP by phone with transaction isolation to prevent race condition
     * Uses SERIALIZABLE isolation to ensure atomic read-verify-clear operation
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean verifyOtp(String phone, String otp) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            log.warn("User not found for phone: {}", phone);
            return false;
        }
        return verifyAndClearOtp(user, otp);
    }

    /**
     * Verify OTP by email with transaction isolation to prevent race condition
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean verifyOtpByEmail(String email, String otp) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("User not found for email: {}", email);
            return false;
        }
        return verifyAndClearOtp(user, otp);
    }
    
    /**
     * Common method to verify OTP and clear it atomically
     */
    private boolean verifyAndClearOtp(User user, String otp) {
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            log.warn("No OTP stored for user: {}", user.getUsername());
            return false;
        }
        
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            log.warn("OTP expired for user: {}", user.getUsername());
            // Clear expired OTP
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            return false;
        }
        
        boolean isValid = user.getOtp().equals(otp);
        if (isValid) {
            // Clear OTP immediately after successful verification (atomic operation)
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            log.info("OTP verified successfully for user: {}", user.getUsername());
        } else {
            log.warn("Invalid OTP for user: {}", user.getUsername());
        }
        return isValid;
    }
    
    /**
     * Clear OTP for a phone number
     */
    @Transactional
    public void clearOtp(String phone) {
        userRepository.findByPhone(phone).ifPresent(u -> {
            u.setOtp(null);
            u.setOtpExpiry(null);
            userRepository.save(u);
        });
    }

    /**
     * Clear OTP for an email
     */
    @Transactional
    public void clearOtpByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setOtp(null);
            u.setOtpExpiry(null);
            userRepository.save(u);
        });
    }
}
