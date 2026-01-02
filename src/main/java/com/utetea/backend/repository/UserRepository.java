package com.utetea.backend.repository;

import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrPhone(String username, String phone);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    
    // FIX B7: Database-level pagination queries
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    // FIX B7: Database-level search với pagination
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.phone LIKE CONCAT('%', :keyword, '%')")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    List<User> findByRole(UserRole role);
    
    // Tìm users theo nhiều roles (cho User Monitoring)
    List<User> findByRoleIn(List<UserRole> roles);
    
    // Tìm managers quản lý store cụ thể
    @Query("SELECT u FROM User u JOIN u.managedStores s WHERE u.role = 'MANAGER' AND s.id = :storeId")
    List<User> findManagersByStoreId(@Param("storeId") Long storeId);
    
    // Tìm user với managed stores (eager load)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.managedStores WHERE u.id = :userId")
    Optional<User> findByIdWithManagedStores(@Param("userId") Long userId);
    
    // Tìm user với managed stores theo username
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.managedStores WHERE u.username = :username")
    Optional<User> findByUsernameWithManagedStores(@Param("username") String username);
    
    // Cộng điểm loyalty cho user
    @Modifying
    @Query("UPDATE User u SET u.points = u.points + :points WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("points") Integer points);
}
