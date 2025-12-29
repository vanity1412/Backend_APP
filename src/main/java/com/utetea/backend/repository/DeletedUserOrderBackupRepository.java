package com.utetea.backend.repository;

import com.utetea.backend.model.DeletedUserOrderBackup;
import com.utetea.backend.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface DeletedUserOrderBackupRepository extends JpaRepository<DeletedUserOrderBackup, Long> {
    
    // Tìm theo user đã xóa
    List<DeletedUserOrderBackup> findByDeletedUserId(Long deletedUserId);
    List<DeletedUserOrderBackup> findByDeletedUsername(String deletedUsername);
    
    // Tính doanh thu từ backup (cho manager dashboard)
    @Query("SELECT SUM(b.finalPrice) FROM DeletedUserOrderBackup b " +
           "WHERE b.orderStatus = :status " +
           "AND b.orderCreatedAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateBackupRevenue(
            @Param("status") OrderStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
    
    // Đếm số đơn hàng backup theo khoảng thời gian
    Long countByOrderCreatedAtBetween(Instant startDate, Instant endDate);
    
    // Lấy danh sách backup theo khoảng thời gian
    Page<DeletedUserOrderBackup> findByOrderCreatedAtBetween(
            Instant startDate, 
            Instant endDate, 
            Pageable pageable);
    
    // Lấy backup theo store
    List<DeletedUserOrderBackup> findByStoreId(Long storeId);
    
    // Thống kê doanh thu theo ngày từ backup
    @Query("SELECT DATE(b.orderCreatedAt) as date, SUM(b.finalPrice) as revenue, COUNT(b) as orderCount " +
           "FROM DeletedUserOrderBackup b " +
           "WHERE b.orderStatus = 'DONE' " +
           "AND b.orderCreatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(b.orderCreatedAt) " +
           "ORDER BY DATE(b.orderCreatedAt)")
    List<Object[]> getDailyRevenueBackup(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
    
    // Thống kê doanh thu theo tháng từ backup
    @Query("SELECT YEAR(b.orderCreatedAt) as year, MONTH(b.orderCreatedAt) as month, " +
           "SUM(b.finalPrice) as revenue, COUNT(b) as orderCount " +
           "FROM DeletedUserOrderBackup b " +
           "WHERE b.orderStatus = 'DONE' " +
           "AND b.orderCreatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(b.orderCreatedAt), MONTH(b.orderCreatedAt) " +
           "ORDER BY YEAR(b.orderCreatedAt), MONTH(b.orderCreatedAt)")
    List<Object[]> getMonthlyRevenueBackup(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
