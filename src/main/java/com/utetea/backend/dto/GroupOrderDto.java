package com.utetea.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime expiresAt;
    
    /**
     * Số giây còn lại trước khi hết hạn (tính từ server)
     * Client dùng trực tiếp để hiển thị countdown
     */
    private Long remainingSeconds;
    
    private Integer maxMembers;
    private Integer currentMemberCount;
    
    private BigDecimal totalPrice;
    private Long finalOrderId;
    
    private List<GroupOrderMemberDto> members;
    private List<GroupOrderItemDto> items;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime updatedAt;
    
    /**
     * Flag để phân biệt phiên mới tạo (true) hay phiên cũ được trả về (false)
     * Dùng khi gọi API createGroupOrder
     */
    private Boolean isNewSession;
}
