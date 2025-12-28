package com.utetea.backend.service;

import com.utetea.backend.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service để gửi thông báo realtime qua WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo đơn hàng mới đến tất cả manager
     */
    public void notifyNewOrder(OrderDto order) {
        log.info("Broadcasting new order #{} to managers", order.getId());
        messagingTemplate.convertAndSend("/topic/orders/new", order);
    }

    /**
     * Gửi thông báo cập nhật trạng thái đơn hàng
     */
    public void notifyOrderStatusUpdate(OrderDto order) {
        log.info("Broadcasting order #{} status update: {}", order.getId(), order.getStatus());
        messagingTemplate.convertAndSend("/topic/orders/status", order);
    }

    /**
     * Gửi thông báo đơn hàng mới đến chi nhánh cụ thể
     */
    public void notifyNewOrderToStore(OrderDto order, Long storeId) {
        log.info("Broadcasting new order #{} to store {}", order.getId(), storeId);
        messagingTemplate.convertAndSend("/topic/orders/store/" + storeId, order);
    }
}
