package com.utetea.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
    
    public InvalidQuantityException(int quantity, int min, int max) {
        super(String.format("Số lượng %d không hợp lệ. Phải từ %d đến %d", quantity, min, max));
    }
}
