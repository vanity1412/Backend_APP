package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnServiceDto {
    @JsonProperty("service_id")
    private int serviceId;
    
    @JsonProperty("short_name")
    private String shortName;
    
    @JsonProperty("service_type_id")
    private int serviceTypeId;
    
    @JsonProperty("config_fee_id")
    private String configFeeId;
    
    @JsonProperty("extra_cost_id")
    private String extraCostId;
    
    @JsonProperty("standard_config_fee_id")
    private String standardConfigFeeId;
    
    @JsonProperty("standard_extra_cost_id")
    private String standardExtraCostId;
}
