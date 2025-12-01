package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.UserDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.OrderRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        log.info("Getting dashboard summary");
        
        try {
            // Count orders by status (all time)
            Long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
            Long completedOrders = orderRepository.countByStatus(OrderStatus.DONE);
            Long canceledOrders = orderRepository.countByStatus(OrderStatus.CANCELED);
            Long totalOrders = orderRepository.count();
            
            // Calculate total revenue (simple sum of all DONE orders)
            BigDecimal totalRevenue = BigDecimal.ZERO;
            // TODO: Implement revenue calculation when needed
            
            // Get top selling drinks (TODO: implement later)
            List<DashboardSummaryDto.TopSellingDrinkDto> topSellingDrinks = new ArrayList<>();
            
            DashboardSummaryDto summary = new DashboardSummaryDto();
            summary.setTotalRevenue(totalRevenue);
            summary.setTotalOrders(totalOrders != null ? totalOrders : 0L);
            summary.setPendingOrders(pendingOrders != null ? pendingOrders : 0L);
            summary.setCompletedOrders(completedOrders != null ? completedOrders : 0L);
            summary.setCanceledOrders(canceledOrders != null ? canceledOrders : 0L);
            summary.setTopSellingDrinks(topSellingDrinks);
            
            log.info("Dashboard summary: revenue={}, total={}, pending={}", 
                totalRevenue, totalOrders, pendingOrders);
            
            return summary;
        } catch (Exception e) {
            log.error("Error getting dashboard summary", e);
            throw e;
        }
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Getting orders with status: {}", status);
        
        if (status == null) {
            return orderService.getAllOrders(pageable);
        }
        
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            .map(order -> orderService.getOrderById(order.getId()));
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Manager updating order {} to status {}", orderId, newStatus);
        return orderService.updateOrderStatus(orderId, newStatus);
    }
    
    // ==================== USER MANAGEMENT ====================
    
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String role, Pageable pageable) {
        log.info("Getting all users with role filter: {}", role);
        
        List<User> users;
        if (role != null && !role.isEmpty()) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                users = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == userRole)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role filter: {}", role);
                users = userRepository.findAll();
            }
        } else {
            users = userRepository.findAll();
        }
        
        // Convert to UserDto
        List<UserDto> userDtos = users.stream()
            .map(UserDto::new)
            .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userDtos.size());
        List<UserDto> pageContent = userDtos.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, userDtos.size());
    }
    
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        log.info("Getting user by id: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return new UserDto(user);
    }
    
    @Transactional
    public UserDto toggleUserBlock(Long userId, boolean blocked) {
        log.info("Toggling user {} block status to: {}", userId, blocked);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setIsBlocked(blocked);
        user.setActive(!blocked); // If blocked, set active to false
        
        User savedUser = userRepository.save(user);
        log.info("User {} block status updated to: {}", userId, blocked);
        
        return new UserDto(savedUser);
    }
    
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}", keyword);
        
        List<User> users = userRepository.findAll().stream()
            .filter(u -> 
                (u.getUsername() != null && u.getUsername().toLowerCase().contains(keyword.toLowerCase())) ||
                (u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword.toLowerCase())) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                (u.getPhone() != null && u.getPhone().contains(keyword))
            )
            .collect(Collectors.toList());
        
        // Convert to UserDto
        List<UserDto> userDtos = users.stream()
            .map(UserDto::new)
            .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userDtos.size());
        List<UserDto> pageContent = userDtos.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, userDtos.size());
    }
}
