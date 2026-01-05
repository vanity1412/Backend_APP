package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GhnWardDto {
    @JsonProperty("WardCode")
    private String wardCode;
    
    @JsonProperty("DistrictID")
    private int districtId;
    
    @JsonProperty("WardName")
    private String wardName;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
    
    @JsonProperty("IsEnable")
    private int isEnable;
    
    @JsonProperty("CanUpdateCOD")
    private boolean canUpdateCOD;
    
    @JsonProperty("SupportType")
    private int supportType;
    
    @JsonProperty("Status")
    private int status;
}
