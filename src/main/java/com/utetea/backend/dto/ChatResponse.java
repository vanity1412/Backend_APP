package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String type; // TEXT, DRINKS, VOUCHERS, STORES, ORDER
    private List<?> data; // Optional data (drinks, vouchers, etc.)
}
