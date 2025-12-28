package com.utetea.backend.repository;

import com.utetea.backend.model.GroupOrder;
import com.utetea.backend.model.GroupOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {
    
    Optional<GroupOrder> findByInviteCode(String inviteCode);
    
    // Tách riêng fetch members và items để tránh MultipleBagFetchException
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "LEFT JOIN FETCH g.members m " +
           "LEFT JOIN FETCH m.user " +
           "LEFT JOIN FETCH g.store " +
           "LEFT JOIN FETCH g.hostUser " +
           "WHERE g.id = :id")
    Optional<GroupOrder> findByIdWithMembers(@Param("id") Long id);
    
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "LEFT JOIN FETCH g.items i " +
           "LEFT JOIN FETCH i.user " +
           "LEFT JOIN FETCH i.drink " +
           "WHERE g.id = :id")
    Optional<GroupOrder> findByIdWithItems(@Param("id") Long id);
    
    // Giữ lại query cũ nhưng chỉ fetch members (không fetch items)
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "LEFT JOIN FETCH g.members m " +
           "LEFT JOIN FETCH m.user " +
           "LEFT JOIN FETCH g.store " +
           "LEFT JOIN FETCH g.hostUser " +
           "WHERE g.id = :id")
    Optional<GroupOrder> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT g FROM GroupOrder g " +
           "LEFT JOIN FETCH g.members m " +
           "LEFT JOIN FETCH m.user " +
           "LEFT JOIN FETCH g.store " +
           "WHERE g.inviteCode = :code")
    Optional<GroupOrder> findByInviteCodeWithMembers(@Param("code") String code);
    
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "JOIN g.members m " +
           "WHERE m.user.id = :userId AND g.status = :status")
    List<GroupOrder> findByMemberUserIdAndStatus(@Param("userId") Long userId, 
                                                  @Param("status") GroupOrderStatus status);
    
    // Lấy cả OPEN và LOCKED
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "JOIN g.members m " +
           "WHERE m.user.id = :userId AND g.status IN (:statuses)")
    List<GroupOrder> findByMemberUserIdAndStatusIn(@Param("userId") Long userId, 
                                                    @Param("statuses") List<GroupOrderStatus> statuses);
    
    @Query("SELECT DISTINCT g FROM GroupOrder g " +
           "JOIN g.members m " +
           "WHERE m.user.id = :userId " +
           "ORDER BY g.createdAt DESC")
    List<GroupOrder> findByMemberUserId(@Param("userId") Long userId);
    
    @Query("SELECT g FROM GroupOrder g WHERE g.hostUser.id = :userId AND g.status = :status")
    List<GroupOrder> findByHostUserIdAndStatus(@Param("userId") Long userId, 
                                                @Param("status") GroupOrderStatus status);
    
    @Query("SELECT g FROM GroupOrder g WHERE g.status = :status AND g.expiresAt < :now")
    List<GroupOrder> findExpiredGroupOrders(@Param("status") GroupOrderStatus status, 
                                            @Param("now") LocalDateTime now);
    
    boolean existsByInviteCode(String inviteCode);
}
