package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    
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
}
