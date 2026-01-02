package com.utetea.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
    COD,
    VNPAY,
    VIETQR;
    
    @JsonValue
    public String getValue() {
        return this.name();
    }
    
    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null) return null;
        return PaymentMethod.valueOf(value.toUpperCase());
    }
}
