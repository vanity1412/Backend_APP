package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GhnDistrictDto {
    @JsonProperty("DistrictID")
    private int districtId;
    
    @JsonProperty("ProvinceID")
    private int provinceId;
    
    @JsonProperty("DistrictName")
    private String districtName;
    
    @JsonProperty("Code")
    private String code;
    
    @JsonProperty("Type")
    private int type;
    
    @JsonProperty("SupportType")
    private int supportType;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
    
    @JsonProperty("IsEnable")
    private int isEnable;
    
    @JsonProperty("CanUpdateCOD")
    private boolean canUpdateCOD;
    
    @JsonProperty("Status")
    private int status;
}
