package com.utetea.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetea.backend.config.GhnConfig;
import com.utetea.backend.dto.ghn.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service tích hợp GHN (Giao Hàng Nhanh) API
 * Hỗ trợ: Lấy địa chỉ, tính phí giao hàng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {
    
    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Getter for config (for testing)
    public GhnConfig getGhnConfig() {
        return ghnConfig;
    }
    
    // Default values cho đồ uống
    private static final int DEFAULT_WEIGHT = 500; // 500 gram
    private static final int DEFAULT_HEIGHT = 20;  // 20 cm
    private static final int DEFAULT_WIDTH = 15;   // 15 cm
    private static final int DEFAULT_LENGTH = 15;  // 15 cm
    private static final int SERVICE_TYPE_STANDARD = 2;
    
    /**
     * Tạo headers cho GHN API
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));
        return headers;
    }
    
    /**
     * Lấy danh sách tỉnh/thành phố
     */
    @Cacheable(value = "ghn-provinces", unless = "#result == null || #result.isEmpty()")
    public List<GhnProvinceDto> getProvinces() {
        String url = ghnConfig.getHost() + "/shiip/public-api/master-data/province";
        
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            
            log.info("Calling GHN API: {}", url);
            log.info("Token: {}...", ghnConfig.getToken().substring(0, Math.min(10, ghnConfig.getToken().length())));
            log.info("ShopId: {}", ghnConfig.getShopId());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.info("GHN Response Status: {}", response.getStatusCode());
            log.info("GHN Response Body: {}", response.getBody());
            
            GhnApiResponse<List<GhnProvinceDto>> result = objectMapper.readValue(
                response.getBody(),
                new TypeReference<GhnApiResponse<List<GhnProvinceDto>>>() {}
            );
            
            if (result.getData() == null || result.getData().isEmpty()) {
                log.error("GHN returned empty data. Message: {}, Code: {}", result.getMessage(), result.getCode());
                throw new RuntimeException("GHN API returned empty data: " + result.getMessage());
            }
            
            log.info("Fetched {} provinces from GHN", result.getData().size());
            return result.getData();
        } catch (Exception e) {
            log.error("Failed to get provinces from GHN: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách tỉnh/thành phố từ GHN: " + e.getMessage(), e);
        }
    }

    
    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    @Cacheable(value = "ghn-districts", key = "#provinceId", unless = "#result == null || #result.isEmpty()")
    public List<GhnDistrictDto> getDistricts(int provinceId) {
        String url = ghnConfig.getHost() + "/shiip/public-api/master-data/district";
        
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("province_id", provinceId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            GhnApiResponse<List<GhnDistrictDto>> result = objectMapper.readValue(
                response.getBody(),
                new TypeReference<GhnApiResponse<List<GhnDistrictDto>>>() {}
            );
            
            log.info("Fetched {} districts for province {} from GHN", 
                result.getData() != null ? result.getData().size() : 0, provinceId);
            return result.getData();
        } catch (Exception e) {
            log.error("Failed to get districts from GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách quận/huyện từ GHN", e);
        }
    }
    
    /**
     * Lấy danh sách phường/xã theo quận
     */
    @Cacheable(value = "ghn-wards", key = "#districtId", unless = "#result == null || #result.isEmpty()")
    public List<GhnWardDto> getWards(int districtId) {
        String url = ghnConfig.getHost() + "/shiip/public-api/master-data/ward";
        
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("district_id", districtId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            GhnApiResponse<List<GhnWardDto>> result = objectMapper.readValue(
                response.getBody(),
                new TypeReference<GhnApiResponse<List<GhnWardDto>>>() {}
            );
            
            log.info("Fetched {} wards for district {} from GHN", 
                result.getData() != null ? result.getData().size() : 0, districtId);
            return result.getData();
        } catch (Exception e) {
            log.error("Failed to get wards from GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách phường/xã từ GHN", e);
        }
    }
    
    /**
     * Lấy danh sách dịch vụ giao hàng có sẵn
     */
    public List<GhnServiceDto> getServiceList(int fromDistrictId, int toDistrictId) {
        String url = ghnConfig.getHost() + "/shiip/public-api/v2/shipping-order/available-services";
        
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("shop_id", ghnConfig.getShopId());
            body.put("from_district", fromDistrictId);
            body.put("to_district", toDistrictId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            GhnApiResponse<List<GhnServiceDto>> result = objectMapper.readValue(
                response.getBody(),
                new TypeReference<GhnApiResponse<List<GhnServiceDto>>>() {}
            );
            
            log.info("Fetched {} services from GHN", result.getData() != null ? result.getData().size() : 0);
            return result.getData();
        } catch (Exception e) {
            log.error("Failed to get service list from GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách dịch vụ giao hàng từ GHN", e);
        }
    }

    
    /**
     * Tính phí giao hàng chi tiết
     */
    public GhnCalculateFeeResponse calculateShippingFee(GhnCalculateFeeRequest request) {
        String url = ghnConfig.getHost() + "/shiip/public-api/v2/shipping-order/fee";
        
        try {
            HttpEntity<GhnCalculateFeeRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            GhnApiResponse<GhnCalculateFeeResponse> result = objectMapper.readValue(
                response.getBody(),
                new TypeReference<GhnApiResponse<GhnCalculateFeeResponse>>() {}
            );
            
            if (result.getData() == null) {
                throw new RuntimeException("GHN API returned null data: " + result.getMessage());
            }
            
            log.info("Calculated shipping fee: {} VND", result.getData().getTotal());
            return result.getData();
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee from GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể tính phí giao hàng từ GHN: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tính phí giao hàng đơn giản (dùng cho app đặt đồ uống)
     * Sử dụng các giá trị mặc định phù hợp với đồ uống
     */
    public GhnCalculateFeeResponse calculateShippingFeeSimple(ShippingFeeRequest request) {
        GhnCalculateFeeRequest ghnRequest = GhnCalculateFeeRequest.builder()
            .toDistrictId(request.getToDistrictId())
            .toWardCode(request.getToWardCode())
            .serviceTypeId(request.getServiceTypeId() != null ? request.getServiceTypeId() : SERVICE_TYPE_STANDARD)
            .weight(request.getWeight() != null ? request.getWeight() : DEFAULT_WEIGHT)
            .height(DEFAULT_HEIGHT)
            .width(DEFAULT_WIDTH)
            .length(DEFAULT_LENGTH)
            .insuranceValue(request.getInsuranceValue())
            .codValue(request.getCodValue())
            .build();
        
        return calculateShippingFee(ghnRequest);
    }
    
    /**
     * Tính phí giao hàng nhanh chóng chỉ với district và ward
     * Trả về tổng phí giao hàng (VND)
     */
    public int getShippingFee(int toDistrictId, String toWardCode) {
        ShippingFeeRequest request = new ShippingFeeRequest();
        request.setToDistrictId(toDistrictId);
        request.setToWardCode(toWardCode);
        
        GhnCalculateFeeResponse response = calculateShippingFeeSimple(request);
        return response.getTotal();
    }
    
    /**
     * Tính phí giao hàng với giá trị đơn hàng (để tính bảo hiểm)
     */
    public int getShippingFeeWithInsurance(int toDistrictId, String toWardCode, int orderValue) {
        ShippingFeeRequest request = new ShippingFeeRequest();
        request.setToDistrictId(toDistrictId);
        request.setToWardCode(toWardCode);
        request.setInsuranceValue(Math.min(orderValue, 5000000)); // Max 5 triệu
        
        GhnCalculateFeeResponse response = calculateShippingFeeSimple(request);
        return response.getTotal();
    }
}
