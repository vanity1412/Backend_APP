package com.utetea.backend.service;

import com.utetea.backend.model.WhitelistedIP;
import com.utetea.backend.repository.WhitelistedIPRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 🔓 Service quản lý Whitelist IP cho Admin/Manager
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhitelistedIPService {

    private final WhitelistedIPRepository whitelistedIPRepository;

    /**
     * Khởi tạo whitelist mặc định (127.0.0.1)
     */
    @PostConstruct
    @Transactional
    public void initDefaultWhitelist() {
        // Thêm localhost vào whitelist nếu chưa có
        if (!whitelistedIPRepository.existsByIpAddress("127.0.0.1")) {
            WhitelistedIP localhost = WhitelistedIP.builder()
                    .ipAddress("127.0.0.1")
                    .description("Localhost - Development")
                    .isActive(true)
                    .build();
            whitelistedIPRepository.save(localhost);
            log.info("✅ Added default whitelist IP: 127.0.0.1");
        }
    }

    /**
     * Kiểm tra IP có trong whitelist không
     */
    public boolean isIPWhitelisted(String ipAddress) {
        if (ipAddress == null) return false;
        
        String normalizedIP = normalizeIP(ipAddress);
        return whitelistedIPRepository.isIPWhitelisted(normalizedIP);
    }

    /**
     * Thêm IP vào whitelist
     */
    @Transactional
    public WhitelistedIP addToWhitelist(String ipAddress, String description, Long addedById) {
        log.info("🔓 Adding IP to whitelist: {} by user: {}", ipAddress, addedById);

        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ IP không được để trống");
        }

        String normalizedIP = normalizeIP(ipAddress.trim());

        // Kiểm tra đã tồn tại chưa
        if (whitelistedIPRepository.existsByIpAddress(normalizedIP)) {
            // Nếu đã tồn tại nhưng inactive, kích hoạt lại
            WhitelistedIP existing = whitelistedIPRepository.findByIpAddressAndIsActiveTrue(normalizedIP)
                    .orElse(null);
            if (existing != null) {
                throw new RuntimeException("IP này đã có trong whitelist");
            }
            
            // Tìm và kích hoạt lại
            List<WhitelistedIP> all = whitelistedIPRepository.findAll();
            for (WhitelistedIP w : all) {
                if (w.getIpAddress().equals(normalizedIP)) {
                    w.setIsActive(true);
                    w.setDescription(description);
                    w.setAddedById(addedById);
                    return whitelistedIPRepository.save(w);
                }
            }
        }

        WhitelistedIP whitelistedIP = WhitelistedIP.builder()
                .ipAddress(normalizedIP)
                .description(description)
                .addedById(addedById)
                .isActive(true)
                .build();

        WhitelistedIP saved = whitelistedIPRepository.save(whitelistedIP);
        log.info("✅ IP {} added to whitelist with id: {}", normalizedIP, saved.getId());

        return saved;
    }

    /**
     * Xóa IP khỏi whitelist (soft delete)
     */
    @Transactional
    public void removeFromWhitelist(Long id) {
        WhitelistedIP whitelistedIP = whitelistedIPRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy whitelist IP"));

        whitelistedIP.setIsActive(false);
        whitelistedIPRepository.save(whitelistedIP);
        log.info("🔒 IP {} removed from whitelist", whitelistedIP.getIpAddress());
    }

    /**
     * Lấy danh sách whitelist đang active
     */
    public List<WhitelistedIP> getActiveWhitelist() {
        return whitelistedIPRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Lấy tất cả whitelist (có phân trang)
     */
    public Page<WhitelistedIP> getAllWhitelist(Pageable pageable) {
        return whitelistedIPRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Normalize IP address
     */
    private String normalizeIP(String ip) {
        if (ip == null) return "unknown";

        // Convert IPv6 localhost to IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }
}
