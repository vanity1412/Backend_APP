package com.utetea.backend.repository;

import com.utetea.backend.model.BlockedIP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 🚫 Repository cho BlockedIP
 */
@Repository
public interface BlockedIPRepository extends JpaRepository<BlockedIP, Long> {

    /**
     * Tìm IP đang bị chặn (active và chưa hết hạn)
     * Bao gồm: PERMANENT, AUTO (vĩnh viễn), TEMPORARY (còn hạn)
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.ipAddress = :ip AND b.isActive = true " +
           "AND (b.blockType = 'PERMANENT' OR b.blockType = 'AUTO' OR (b.blockType = 'TEMPORARY' AND b.blockedUntil > :now))")
    Optional<BlockedIP> findActiveBlockedIP(@Param("ip") String ip, @Param("now") Instant now);

    /**
     * Kiểm tra IP có đang bị chặn không
     */
    @Query("SELECT COUNT(b) > 0 FROM BlockedIP b WHERE b.ipAddress = :ip AND b.isActive = true " +
           "AND (b.blockType = 'PERMANENT' OR b.blockType = 'AUTO' OR (b.blockType = 'TEMPORARY' AND b.blockedUntil > :now))")
    boolean isIPBlocked(@Param("ip") String ip, @Param("now") Instant now);

    /**
     * Lấy danh sách IP đang bị chặn
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.isActive = true " +
           "AND (b.blockType = 'PERMANENT' OR b.blockType = 'AUTO' OR (b.blockType = 'TEMPORARY' AND b.blockedUntil > :now)) " +
           "ORDER BY b.createdAt DESC")
    Page<BlockedIP> findActiveBlockedIPs(@Param("now") Instant now, Pageable pageable);

    /**
     * Lấy tất cả blocked IPs (bao gồm đã gỡ)
     */
    Page<BlockedIP> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tìm theo IP address
     */
    List<BlockedIP> findByIpAddressContainingIgnoreCaseOrderByCreatedAtDesc(String ip);

    /**
     * Tìm theo user liên quan
     */
    List<BlockedIP> findByRelatedUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Đếm số IP đang bị chặn
     */
    @Query("SELECT COUNT(b) FROM BlockedIP b WHERE b.isActive = true " +
           "AND (b.blockType = 'PERMANENT' OR b.blockType = 'AUTO' OR (b.blockType = 'TEMPORARY' AND b.blockedUntil > :now))")
    long countActiveBlockedIPs(@Param("now") Instant now);

    /**
     * Lấy các IP hết hạn cần cập nhật
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.isActive = true " +
           "AND b.blockType = 'TEMPORARY' AND b.blockedUntil <= :now")
    List<BlockedIP> findExpiredBlocks(@Param("now") Instant now);
}
