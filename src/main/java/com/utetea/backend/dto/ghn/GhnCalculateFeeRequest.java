package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GhnCalculateFeeRequest {
    /**
     * District ID nơi giao hàng (bắt buộc)
     */
    @JsonProperty("to_district_id")
    private int toDistrictId;
    
    /**
     * Ward code nơi giao hàng (bắt buộc)
     */
    @JsonProperty("to_ward_code")
    private String toWardCode;
    
    /**
     * Service ID - lấy từ API getServiceList
     */
    @JsonProperty("service_id")
    private Integer serviceId;
    
    /**
     * Service Type ID: 1 = Express, 2 = Standard
     */
    @JsonProperty("service_type_id")
    private Integer serviceTypeId;
    
    /**
     * District ID nơi lấy hàng (nếu không nhập sẽ lấy từ shopId)
     */
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;
    
    /**
     * Ward code nơi lấy hàng (nếu không nhập sẽ lấy từ shopId)
     */
    @JsonProperty("from_ward_code")
    private String fromWardCode;
    
    /**
     * Chiều cao (cm)
     */
    private int height;
    
    /**
     * Cân nặng (gram)
     */
    private int weight;
    
    /**
     * Chiều rộng (cm)
     */
    private int width;
    
    /**
     * Chiều dài (cm)
     */
    private int length;
    
    /**
     * Giá trị hàng hóa để bảo hiểm (tối đa 5.000.000, mặc định 0)
     */
    @JsonProperty("insurance_value")
    private Integer insuranceValue;
    
    /**
     * Mã coupon giảm giá
     */
    private String coupon;
    
    /**
     * Giá trị thu hộ khi giao thất bại
     */
    @JsonProperty("cod_failed_amount")
    private Integer codFailedAmount;
    
    /**
     * Số tiền thu hộ COD (tối đa 5.000.000, mặc định 0)
     */
    @JsonProperty("cod_value")
    private Integer codValue;
    
    /**
     * Danh sách items
     */
    private List<GhnItemDto> items;
}
