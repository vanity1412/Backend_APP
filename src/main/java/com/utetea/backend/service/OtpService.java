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
    private final SendGridEmailService sendGridEmailService;
    
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
     * Send OTP via email - tries SendGrid first, then falls back to SMTP
     */
    public void sendOtp(String otp, String email) {
        log.info("Preparing to send OTP email to: {}", email);
        
        String subject = "[UTE Tea] Mã OTP xác thực tài khoản";
        String content = "Xin chào,\n\n" +
                "Chúng tôi đã nhận được yêu cầu xác thực tài khoản của bạn tại UTE Tea.\n\n" +
                "----------------------------------------\n" +
                "MÃ OTP CỦA BẠN:  " + otp + "\n" +
                "----------------------------------------\n\n" +
                "⏳ Mã OTP có hiệu lực trong " + OTP_VALIDITY_MINUTES + " phút.\n" +
                "🔒 Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n" +
                "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email hoặc liên hệ với chúng tôi để được hỗ trợ.\n\n" +
                "Trân trọng,\n" +
                "UTE Tea Team\n" +
                "----------------------------------------\n" +
                "Email hỗ trợ: watershoputetea@gmail.com";

        // Try SendGrid first (better for cloud platforms)
        if (sendGridEmailService.isEnabled()) {
            log.info("Attempting to send OTP via SendGrid...");
            if (sendGridEmailService.sendEmail(email, subject, content)) {
                log.info("OTP sent successfully via SendGrid to: {}", email);
                return;
            }
            log.warn("SendGrid failed, falling back to SMTP...");
        }

        // Fallback to SMTP
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("watershoputetea@gmail.com");
            message.setTo(email);
            message.setSubject(subject);
            message.setText(content);

            log.debug("Attempting to send email via SMTP...");
            mailSender.send(message);
            log.info("OTP email sent successfully via SMTP to: {}", email);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.error("SMTP Authentication failed! Check Gmail App Password. Error: {}", e.getMessage());
            throw new RuntimeException("Lỗi xác thực email. Vui lòng liên hệ admin.");
        } catch (org.springframework.mail.MailSendException e) {
            log.error("Failed to send email. SMTP connection issue. Error: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Could not connect")) {
                throw new RuntimeException("Không thể kết nối đến máy chủ email. Vui lòng thử lại sau.");
            }
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi email OTP: " + e.getMessage());
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
