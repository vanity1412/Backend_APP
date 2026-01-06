package com.utetea.backend.dto;

import com.utetea.backend.model.GroupOrderStatus;
import com.utetea.backend.model.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrderDto {
    private Long id;
    private String inviteCode;
    private String name;
    private GroupOrderStatus status;
    
    private Long hostUserId;
    private String hostUserName;
    
    private Long storeId;
    private String storeName;
    
    private OrderType orderType;
    private String deliveryAddress;
    
    private LocalDateTime expiresAt;
    private Integer maxMembers;
    private Integer currentMemberCount;
    
    private BigDecimal totalPrice;
    private Long finalOrderId;
    
    private List<GroupOrderMemberDto> members;
    private List<GroupOrderItemDto> items;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Flag để phân biệt phiên mới tạo (true) hay phiên cũ được trả về (false)
     * Dùng khi gọi API createGroupOrder
     */
    private Boolean isNewSession;
}
