package com.utetea.backend.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM;
    
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
