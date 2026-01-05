package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GhnProvinceDto {
    @JsonProperty("ProvinceID")
    private int provinceId;
    
    @JsonProperty("ProvinceName")
    private String provinceName;
    
    @JsonProperty("CountryID")
    private int countryId;
    
    @JsonProperty("Code")
    private String code;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
    
    @JsonProperty("IsEnable")
    private int isEnable;
    
    @JsonProperty("RegionID")
    private int regionId;
    
    @JsonProperty("Status")
    private int status;
}
