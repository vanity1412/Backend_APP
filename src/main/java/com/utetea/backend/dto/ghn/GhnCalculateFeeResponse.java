package com.utetea.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnCalculateFeeResponse {
    /**
     * Tổng phí giao hàng
     */
    private int total;
    
    /**
     * Phí dịch vụ
     */
    @JsonProperty("service_fee")
    private int serviceFee;
    
    /**
     * Phí bảo hiểm
     */
    @JsonProperty("insurance_fee")
    private int insuranceFee;
    
    /**
     * Phí điểm giao nhận
     */
    @JsonProperty("pick_station_fee")
    private int pickStationFee;
    
    /**
     * Giá trị coupon giảm giá
     */
    @JsonProperty("coupon_value")
    private int couponValue;
    
    /**
     * Phí R2S
     */
    @JsonProperty("r2s_fee")
    private int r2sFee;
    
    /**
     * Phí trả chứng từ
     */
    @JsonProperty("document_return")
    private int documentReturn;
    
    /**
     * Phí kiểm tra hàng
     */
    @JsonProperty("double_check")
    private int doubleCheck;
    
    /**
     * Phí COD
     */
    @JsonProperty("cod_fee")
    private int codFee;
    
    /**
     * Phí vùng xa lấy hàng
     */
    @JsonProperty("pick_remote_areas_fee")
    private int pickRemoteAreasFee;
    
    /**
     * Phí vùng xa giao hàng
     */
    @JsonProperty("deliver_remote_areas_fee")
    private int deliverRemoteAreasFee;
    
    /**
     * Phí COD thất bại
     */
    @JsonProperty("cod_failed_fee")
    private int codFailedFee;
}
