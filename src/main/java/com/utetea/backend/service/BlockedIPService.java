package com.utetea.backend.service;

import com.utetea.backend.dto.BlockedIPDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.BlockedIP;
import com.utetea.backend.model.BlockedIP.BlockType;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.BlockedIPRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 🚫 BLOCKED IP SERVICE
 * Quản lý danh sách IP bị chặn
 * Sử dụng cache in-memory để tối ưu performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedIPService {

    private final BlockedIPRepository blockedIPRepository;
    private final UserRepository userRepository;

    // 🚀 Cache IP bị block để check nhanh (không cần query DB mỗi request)
    private final Set<String> blockedIPCache = ConcurrentHashMap.newKeySet();
    private volatile boolean cacheInitialized = false;

    /**
     * Khởi tạo cache khi service start
     */
    @jakarta.annotation.PostConstruct
    public void initCache() {
        refreshCache();
    }

    /**
     * Refresh cache từ database
     */
    public void refreshCache() {
        try {
            List<BlockedIP> activeBlocked = blockedIPRepository.findAllActiveBlocked(LocalDateTime.now());
            blockedIPCache.clear();
            blockedIPCache.addAll(activeBlocked.stream()
                .map(BlockedIP::getIpAddress)
                .collect(Collectors.toSet()));
            cacheInitialized = true;
            log.info("Blocked IP cache refreshed. {} IPs blocked", blockedIPCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh blocked IP cache", e);
        }
    }

    /**
     * 🚀 Check nhanh IP có bị block không (dùng cache)
     */
    public boolean isIPBlocked(String ipAddress) {
        if (!cacheInitialized) {
            refreshCache();
        }
        return blockedIPCache.contains(ipAddress);
    }

    /**
     * Check IP và tăng counter nếu bị block
     */
    @Transactional
    public boolean checkAndIncrementIfBlocked(String ipAddress) {
        if (!isIPBlocked(ipAddress)) {
            return false;
        }
        
        // Tăng counter trong DB (async để không block request)
        try {
            blockedIPRepository.findActiveBlockedIP(ipAddress, LocalDateTime.now())
                .ifPresent(blocked -> {
                    blocked.incrementBlockedCount();
                    blockedIPRepository.save(blocked);
                });
        } catch (Exception e) {
            log.error("Failed to increment blocked count for IP: {}", ipAddress, e);
        }
        
        return true;
    }

    /**
     * 🚫 Block một IP
     */
    @Transactional
    public BlockedIPDto blockIP(String ipAddress, BlockType blockType, String reason, 
                                 Integer durationHours, Long relatedUserId, Long alertId) {
        log.info("Blocking IP: {} - Type: {} - Reason: {}", ipAddress, blockType, reason);
        
        // Validate IP format (basic)
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new BusinessException("IP address is required");
        }
        
        // Check nếu IP đã bị block
        if (isIPBlocked(ipAddress)) {
            throw new BusinessException("IP " + ipAddress + " đã bị block");
        }
        
        User currentUser = getCurrentUser();
        User relatedUser = null;
        if (relatedUserId != null) {
            relatedUser = userRepository.findById(relatedUserId).orElse(null);
        }
        
        LocalDateTime blockedUntil = null;
        if (blockType == BlockType.TEMPORARY && durationHours != null && durationHours > 0) {
            blockedUntil = LocalDateTime.now().plusHours(durationHours);
        }
        
        BlockedIP blocked = BlockedIP.builder()
            .ipAddress(ipAddress.trim())
            .blockType(blockType)
            .reason(reason)
            .blockedBy(currentUser)
            .blockedUntil(blockedUntil)
            .isActive(true)
            .alertId(alertId)
            .relatedUser(relatedUser)
            .blockedRequestsCount(0L)
            .build();
        
        blocked = blockedIPRepository.save(blocked);
        
        // Update cache
        blockedIPCache.add(ipAddress.trim());
        
        log.info("IP {} blocked successfully. ID: {}", ipAddress, blocked.getId());
        return BlockedIPDto.fromEntity(blocked);
    }

    /**
     * 🔓 Unblock một IP
     */
    @Transactional
    public BlockedIPDto unblockIP(Long blockedIPId, String reason) {
        log.info("Unblocking IP ID: {} - Reason: {}", blockedIPId, reason);
        
        BlockedIP blocked = blockedIPRepository.findById(blockedIPId)
            .orElseThrow(() -> new ResourceNotFoundException("Blocked IP not found: " + blockedIPId));
        
        if (!blocked.getIsActive()) {
            throw new BusinessException("IP này đã được unblock trước đó");
        }
        
        User currentUser = getCurrentUser();
        
        blocked.setIsActive(false);
        blocked.setUnblockedAt(LocalDateTime.now());
        blocked.setUnblockedBy(currentUser);
        blocked.setUnblockReason(reason);
        
        blocked = blockedIPRepository.save(blocked);
        
        // Update cache
        blockedIPCache.remove(blocked.getIpAddress());
        
        log.info("IP {} unblocked successfully", blocked.getIpAddress());
        return BlockedIPDto.fromEntity(blocked);
    }

    /**
     * Lấy danh sách IP đang bị block
     */
    @Transactional(readOnly = true)
    public Page<BlockedIPDto> getActiveBlockedIPs(Pageable pageable) {
        return blockedIPRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
            .map(BlockedIPDto::fromEntity);
    }

    /**
     * Lấy tất cả lịch sử block IP
     */
    @Transactional(readOnly = true)
    public Page<BlockedIPDto> getAllBlockedIPs(Pageable pageable) {
        return blockedIPRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(BlockedIPDto::fromEntity);
    }

    /**
     * Lấy thông tin chi tiết một blocked IP
     */
    @Transactional(readOnly = true)
    public BlockedIPDto getBlockedIPById(Long id) {
        return blockedIPRepository.findById(id)
            .map(BlockedIPDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Blocked IP not found: " + id));
    }

    /**
     * Tìm kiếm theo IP
     */
    @Transactional(readOnly = true)
    public List<BlockedIPDto> searchByIP(String ipAddress) {
        return blockedIPRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress)
            .stream()
            .map(BlockedIPDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Lấy IP blocks liên quan đến user
     */
    @Transactional(readOnly = true)
    public List<BlockedIPDto> getBlockedIPsByUser(Long userId) {
        return blockedIPRepository.findByRelatedUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(BlockedIPDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Thống kê
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        long activeCount = blockedIPRepository.countActiveBlocked(LocalDateTime.now());
        long totalCount = blockedIPRepository.count();
        
        return Map.of(
            "activeBlockedIPs", activeCount,
            "totalBlockedIPs", totalCount,
            "cacheSize", blockedIPCache.size()
        );
    }

    /**
     * 🕐 Scheduled task: Cleanup expired blocks và refresh cache
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    @Transactional
    public void cleanupExpiredBlocks() {
        try {
            List<BlockedIP> expired = blockedIPRepository.findExpiredBlocks(LocalDateTime.now());
            if (!expired.isEmpty()) {
                for (BlockedIP blocked : expired) {
                    blocked.setIsActive(false);
                    blocked.setUnblockedAt(LocalDateTime.now());
                    blocked.setUnblockReason("Tự động hết hạn");
                    blockedIPRepository.save(blocked);
                    blockedIPCache.remove(blocked.getIpAddress());
                }
                log.info("Cleaned up {} expired IP blocks", expired.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired IP blocks", e);
        }
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
