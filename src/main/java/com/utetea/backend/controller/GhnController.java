package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.ghn.*;
import com.utetea.backend.service.GhnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho GHN (Giao Hàng Nhanh) API
 * Cung cấp các endpoint để lấy địa chỉ và tính phí giao hàng
 */
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Tag(name = "GHN Shipping", description = "API tích hợp Giao Hàng Nhanh")
public class GhnController {
    
    private final GhnService ghnService;
    
    // ==================== ADDRESS APIs ====================
    
    @GetMapping("/test-config")
    @Operation(summary = "Test GHN configuration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", ghnService.getGhnConfig().getHost());
        config.put("shopId", ghnService.getGhnConfig().getShopId());
        config.put("testMode", ghnService.getGhnConfig().isTestMode());
        config.put("hasToken", ghnService.getGhnConfig().getToken() != null && !ghnService.getGhnConfig().getToken().isEmpty());
        return ResponseEntity.ok(new ApiResponse<>(true, "GHN config loaded", config));
    }
    
    @GetMapping("/provinces")
    @Operation(summary = "Lấy danh sách tỉnh/thành phố")
    public ResponseEntity<ApiResponse<List<GhnProvinceDto>>> getProvinces() {
        List<GhnProvinceDto> provinces = ghnService.getProvinces();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách tỉnh/thành phố thành công", provinces));
    }
    
    @GetMapping("/districts/{provinceId}")
    @Operation(summary = "Lấy danh sách quận/huyện theo tỉnh")
    public ResponseEntity<ApiResponse<List<GhnDistrictDto>>> getDistricts(@PathVariable int provinceId) {
        List<GhnDistrictDto> districts = ghnService.getDistricts(provinceId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách quận/huyện thành công", districts));
    }
    
    @GetMapping("/wards/{districtId}")
    @Operation(summary = "Lấy danh sách phường/xã theo quận")
    public ResponseEntity<ApiResponse<List<GhnWardDto>>> getWards(@PathVariable int districtId) {
        List<GhnWardDto> wards = ghnService.getWards(districtId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách phường/xã thành công", wards));
    }
    
    // ==================== SHIPPING FEE APIs ====================
    
    @GetMapping("/services")
    @Operation(summary = "Lấy danh sách dịch vụ giao hàng có sẵn")
    public ResponseEntity<ApiResponse<List<GhnServiceDto>>> getServices(
            @RequestParam int fromDistrictId,
            @RequestParam int toDistrictId) {
        List<GhnServiceDto> services = ghnService.getServiceList(fromDistrictId, toDistrictId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách dịch vụ thành công", services));
    }
    
    @PostMapping("/calculate-fee")
    @Operation(summary = "Tính phí giao hàng chi tiết")
    public ResponseEntity<ApiResponse<GhnCalculateFeeResponse>> calculateFee(
            @RequestBody GhnCalculateFeeRequest request) {
        GhnCalculateFeeResponse response = ghnService.calculateShippingFee(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tính phí giao hàng thành công", response));
    }
    
    @PostMapping("/calculate-fee/simple")
    @Operation(summary = "Tính phí giao hàng đơn giản (cho app đồ uống)")
    public ResponseEntity<ApiResponse<GhnCalculateFeeResponse>> calculateFeeSimple(
            @RequestBody ShippingFeeRequest request) {
        GhnCalculateFeeResponse response = ghnService.calculateShippingFeeSimple(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tính phí giao hàng thành công", response));
    }
    
    @GetMapping("/shipping-fee")
    @Operation(summary = "Lấy phí giao hàng nhanh")
    public ResponseEntity<ApiResponse<Integer>> getShippingFee(
            @RequestParam int toDistrictId,
            @RequestParam String toWardCode) {
        int fee = ghnService.getShippingFee(toDistrictId, toWardCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy phí giao hàng thành công", fee));
    }
    
    @GetMapping("/shipping-fee/with-insurance")
    @Operation(summary = "Lấy phí giao hàng có bảo hiểm")
    public ResponseEntity<ApiResponse<Integer>> getShippingFeeWithInsurance(
            @RequestParam int toDistrictId,
            @RequestParam String toWardCode,
            @RequestParam int orderValue) {
        int fee = ghnService.getShippingFeeWithInsurance(toDistrictId, toWardCode, orderValue);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy phí giao hàng thành công", fee));
    }
}
