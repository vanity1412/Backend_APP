package com.utetea.backend.dto.ghn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GhnItemDto {
    private String name;
    private String code;
    private int quantity;
    private int height;
    private int weight;
    private int width;
    private int length;
}
