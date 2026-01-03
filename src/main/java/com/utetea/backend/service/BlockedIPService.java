package com.utetea.backend.service;

import com.utetea.backend.dto.BlockIPRequest;
import com.utetea.backend.dto.BlockedIPDto;
import com.utetea.backend.model.BlockedIP;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.BlockedIPRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🚫 Service quản lý Blocked IP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedIPService {

    private final BlockedIPRepository blockedIPRepository;
    private final UserRepository userRepository;

    /**
     * 🚫 Block một IP
     */
    @Transactional
    public BlockedIP blockIP(BlockIPRequest request, Long blockedById) {
        log.info("🚫 Blocking IP: {} by user: {}", request.getIpAddress(), blockedById);
        
        // Kiểm tra IP đã bị block chưa
        Optional<BlockedIP> existing = blockedIPRepository.findActiveBlockedIP(
            request.getIpAddress(), Instant.now());
        
        if (existing.isPresent()) {
            log.warn("IP {} is already blocked", request.getIpAddress());
            throw new RuntimeException("IP này đã bị chặn");
        }
        
        BlockedIP.BlockType blockType = BlockedIP.BlockType.valueOf(request.getBlockType());
        
        Instant blockedUntil = null;
        if (blockType == BlockedIP.BlockType.TEMPORARY && request.getDurationHours() != null) {
            blockedUntil = Instant.now().plus(request.getDurationHours(), ChronoUnit.HOURS);
        }
        
        BlockedIP blockedIP = BlockedIP.builder()
                .ipAddress(request.getIpAddress())
                .blockType(blockType)
                .reason(request.getReason())
                .blockedById(blockedById)
                .blockedUntil(blockedUntil)
                .isActive(true)
                .relatedUserId(request.getRelatedUserId())
                .alertId(request.getAlertId())
                .blockedRequestsCount(0L)
                .build();
        
        return blockedIPRepository.save(blockedIP);
    }

    /**
     * 🔓 Gỡ chặn IP
     */
    @Transactional
    public BlockedIP unblockIP(Long blockedIPId, Long unblockedById, String reason) {
        log.info("🔓 Unblocking IP id: {} by user: {}", blockedIPId, unblockedById);
        
        BlockedIP blockedIP = blockedIPRepository.findById(blockedIPId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy blocked IP"));
        
        blockedIP.setIsActive(false);
        blockedIP.setUnblockedAt(Instant.now());
        blockedIP.setUnblockedById(unblockedById);
        blockedIP.setUnblockReason(reason);
        
        return blockedIPRepository.save(blockedIP);
    }

    /**
     * ✅ Kiểm tra IP có bị chặn không
     */
    public boolean isIPBlocked(String ipAddress) {
        return blockedIPRepository.isIPBlocked(ipAddress, Instant.now());
    }

    /**
     * ✅ Kiểm tra IP có bị chặn không và tăng counter nếu bị chặn
     */
    @Transactional
    public boolean checkAndIncrementIfBlocked(String ipAddress) {
        Optional<BlockedIP> blocked = blockedIPRepository.findActiveBlockedIP(ipAddress, Instant.now());
        if (blocked.isPresent()) {
            blocked.get().incrementBlockedCount();
            blockedIPRepository.save(blocked.get());
            return true;
        }
        return false;
    }

    /**
     * 📊 Tăng số request bị chặn
     */
    @Transactional
    public void incrementBlockedCount(String ipAddress) {
        blockedIPRepository.findActiveBlockedIP(ipAddress, Instant.now())
                .ifPresent(blockedIP -> {
                    blockedIP.incrementBlockedCount();
                    blockedIPRepository.save(blockedIP);
                });
    }

    /**
     * 📋 Lấy danh sách IP đang bị chặn
     */
    public Page<BlockedIPDto> getActiveBlockedIPs(Pageable pageable) {
        return blockedIPRepository.findActiveBlockedIPs(Instant.now(), pageable)
                .map(this::toDto);
    }

    /**
     * 📋 Lấy tất cả blocked IPs
     */
    public Page<BlockedIPDto> getAllBlockedIPs(Pageable pageable) {
        return blockedIPRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    /**
     * 🔍 Tìm kiếm theo IP
     */
    public List<BlockedIPDto> searchByIP(String ip) {
        return blockedIPRepository.findByIpAddressContainingIgnoreCaseOrderByCreatedAtDesc(ip)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 📊 Thống kê
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActive", blockedIPRepository.countActiveBlockedIPs(Instant.now()));
        stats.put("total", blockedIPRepository.count());
        return stats;
    }

    /**
     * 🔄 Tự động gỡ chặn các IP hết hạn (chạy mỗi phút)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredBlocks() {
        List<BlockedIP> expired = blockedIPRepository.findExpiredBlocks(Instant.now());
        if (!expired.isEmpty()) {
            log.info("🔄 Auto-unblocking {} expired IPs", expired.size());
            expired.forEach(ip -> {
                ip.setIsActive(false);
                ip.setUnblockedAt(Instant.now());
                ip.setUnblockReason("Tự động gỡ - Hết hạn");
            });
            blockedIPRepository.saveAll(expired);
        }
    }

    /**
     * Convert entity to DTO
     */
    private BlockedIPDto toDto(BlockedIP entity) {
        String blockedByUsername = null;
        String unblockedByUsername = null;
        String relatedUsername = null;
        
        if (entity.getBlockedById() != null) {
            blockedByUsername = userRepository.findById(entity.getBlockedById())
                    .map(User::getUsername)
                    .orElse(null);
        }
        
        if (entity.getUnblockedById() != null) {
            unblockedByUsername = userRepository.findById(entity.getUnblockedById())
                    .map(User::getUsername)
                    .orElse(null);
        }
        
        if (entity.getRelatedUserId() != null) {
            relatedUsername = userRepository.findById(entity.getRelatedUserId())
                    .map(User::getUsername)
                    .orElse(null);
        }
        
        return BlockedIPDto.fromEntity(entity, blockedByUsername, unblockedByUsername, relatedUsername);
    }
}
