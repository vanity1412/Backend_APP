package com.utetea.backend.dto.ghn;

import lombok.Data;

@Data
public class GhnApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
