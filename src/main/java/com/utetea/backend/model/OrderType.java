package com.utetea.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderType {
    DELIVERY,
    PICKUP;
    
    @JsonValue
    public String getValue() {
        return this.name();
    }
    
    @JsonCreator
    public static OrderType fromString(String value) {
        if (value == null) return null;
        return OrderType.valueOf(value.toUpperCase());
    }
}
