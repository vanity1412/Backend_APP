package com.utetea.backend.repository;

import com.utetea.backend.model.WhitelistedIP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhitelistedIPRepository extends JpaRepository<WhitelistedIP, Long> {

    /**
     * Kiểm tra IP có trong whitelist không
     */
    @Query("SELECT COUNT(w) > 0 FROM WhitelistedIP w WHERE w.ipAddress = :ip AND w.isActive = true")
    boolean isIPWhitelisted(String ip);

    /**
     * Tìm whitelist entry theo IP
     */
    Optional<WhitelistedIP> findByIpAddressAndIsActiveTrue(String ipAddress);

    /**
     * Lấy tất cả IP đang active trong whitelist
     */
    List<WhitelistedIP> findByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Lấy tất cả whitelist (có phân trang)
     */
    Page<WhitelistedIP> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Kiểm tra IP đã tồn tại chưa
     */
    boolean existsByIpAddress(String ipAddress);
}
