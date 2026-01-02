package com.utetea.backend.repository;

import com.utetea.backend.model.BlockedIP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedIPRepository extends JpaRepository<BlockedIP, Long> {

    /**
     * Tìm IP đang bị block (active và chưa hết hạn)
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.ipAddress = :ip AND b.isActive = true " +
           "AND (b.blockedUntil IS NULL OR b.blockedUntil > :now)")
    Optional<BlockedIP> findActiveBlockedIP(@Param("ip") String ipAddress, @Param("now") LocalDateTime now);

    /**
     * Kiểm tra IP có đang bị block không
     */
    @Query("SELECT COUNT(b) > 0 FROM BlockedIP b WHERE b.ipAddress = :ip AND b.isActive = true " +
           "AND (b.blockedUntil IS NULL OR b.blockedUntil > :now)")
    boolean isIPBlocked(@Param("ip") String ipAddress, @Param("now") LocalDateTime now);

    /**
     * Lấy tất cả IP đang bị block
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.isActive = true " +
           "AND (b.blockedUntil IS NULL OR b.blockedUntil > :now) " +
           "ORDER BY b.createdAt DESC")
    List<BlockedIP> findAllActiveBlocked(@Param("now") LocalDateTime now);

    /**
     * Lấy danh sách IP bị block (có phân trang)
     */
    Page<BlockedIP> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy tất cả (bao gồm đã unblock)
     */
    Page<BlockedIP> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tìm theo IP
     */
    List<BlockedIP> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Tìm IP block liên quan đến user
     */
    List<BlockedIP> findByRelatedUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Đếm số IP đang bị block
     */
    @Query("SELECT COUNT(b) FROM BlockedIP b WHERE b.isActive = true " +
           "AND (b.blockedUntil IS NULL OR b.blockedUntil > :now)")
    long countActiveBlocked(@Param("now") LocalDateTime now);

    /**
     * Lấy các IP hết hạn block (để cleanup)
     */
    @Query("SELECT b FROM BlockedIP b WHERE b.isActive = true " +
           "AND b.blockedUntil IS NOT NULL AND b.blockedUntil <= :now")
    List<BlockedIP> findExpiredBlocks(@Param("now") LocalDateTime now);
}
