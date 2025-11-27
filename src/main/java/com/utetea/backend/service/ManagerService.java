package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.Order;
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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        // Count orders by status
        Map<String, Long> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", orderRepository.countByStatus(OrderStatus.PENDING));
        ordersByStatus.put("MAKING", orderRepository.countByStatus(OrderStatus.MAKING));
        ordersByStatus.put("SHIPPING", orderRepository.countByStatus(OrderStatus.SHIPPING));
        ordersByStatus.put("DONE", orderRepository.countByStatus(OrderStatus.DONE));
        ordersByStatus.put("CANCELED", orderRepository.countByStatus(OrderStatus.CANCELED));
        
        // Calculate revenue
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(
                OrderStatus.DONE, startDate, endDate);
        
        BigDecimal todayRevenue = orderRepository.calculateTotalRevenue(
                OrderStatus.DONE, 
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now());
        
        Long totalOrders = orderRepository.countByCreatedAtBetween(startDate, endDate);
        Long todayOrders = orderRepository.countByCreatedAtBetween(
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now());
        
        return DashboardSummaryDto.builder()
                .ordersByStatus(ordersByStatus)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .todayOrders(todayOrders)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(OrderStatus status, LocalDateTime startDate, 
                                       LocalDateTime endDate, Pageable pageable) {
        Page<Order> orders;
        
        if (status != null && startDate != null && endDate != null) {
            orders = orderRepository.findByStatusAndCreatedAtBetween(status, startDate, endDate, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else if (startDate != null && endDate != null) {
            orders = orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        return orders.map(order -> orderService.getOrderById(order.getId()));
    }
    
    @Transactional(readOnly = true)
    public OrderDto getOrderDetail(Long id) {
        return orderService.getOrderById(id);
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Manager updating order {} status to {}", id, newStatus);
        return orderService.updateOrderStatus(id, newStatus);
    }
}
