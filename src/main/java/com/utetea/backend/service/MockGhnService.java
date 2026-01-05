package com.utetea.backend.service;

import com.utetea.backend.dto.ghn.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock GHN Service for testing when real GHN token is not available
 * Enable by setting: spring.profiles.active=mock-ghn
 */
@Service
@Profile("mock-ghn")
@Slf4j
public class MockGhnService extends GhnService {
    
    public MockGhnService(com.utetea.backend.config.GhnConfig ghnConfig) {
        super(ghnConfig);
        log.warn("⚠️ Using MOCK GHN Service - For testing only!");
    }
    
    @Override
    public List<GhnProvinceDto> getProvinces() {
        log.info("Mock: Returning provinces");
        List<GhnProvinceDto> provinces = new ArrayList<>();
        
        GhnProvinceDto hanoi = new GhnProvinceDto();
        hanoi.setProvinceId(201);
        hanoi.setProvinceName("Hà Nội");
        provinces.add(hanoi);
        
        GhnProvinceDto hcm = new GhnProvinceDto();
        hcm.setProvinceId(202);
        hcm.setProvinceName("Hồ Chí Minh");
        provinces.add(hcm);
        
        GhnProvinceDto danang = new GhnProvinceDto();
        danang.setProvinceId(203);
        danang.setProvinceName("Đà Nẵng");
        provinces.add(danang);
        
        return provinces;
    }
    
    @Override
    public List<GhnDistrictDto> getDistricts(int provinceId) {
        log.info("Mock: Returning districts for province {}", provinceId);
        List<GhnDistrictDto> districts = new ArrayList<>();
        
        if (provinceId == 201) { // Hà Nội
            GhnDistrictDto baDinh = new GhnDistrictDto();
            baDinh.setDistrictId(1542);
            baDinh.setDistrictName("Ba Đình");
            baDinh.setProvinceId(201);
            districts.add(baDinh);
            
            GhnDistrictDto hoanKiem = new GhnDistrictDto();
            hoanKiem.setDistrictId(1543);
            hoanKiem.setDistrictName("Hoàn Kiếm");
            hoanKiem.setProvinceId(201);
            districts.add(hoanKiem);
        } else if (provinceId == 202) { // HCM
            GhnDistrictDto quan1 = new GhnDistrictDto();
            quan1.setDistrictId(1444);
            quan1.setDistrictName("Quận 1");
            quan1.setProvinceId(202);
            districts.add(quan1);
            
            GhnDistrictDto quan3 = new GhnDistrictDto();
            quan3.setDistrictId(1446);
            quan3.setDistrictName("Quận 3");
            quan3.setProvinceId(202);
            districts.add(quan3);
        }
        
        return districts;
    }
    
    @Override
    public List<GhnWardDto> getWards(int districtId) {
        log.info("Mock: Returning wards for district {}", districtId);
        List<GhnWardDto> wards = new ArrayList<>();
        
        GhnWardDto ward1 = new GhnWardDto();
        ward1.setWardCode("21012");
        ward1.setWardName("Phường Cống Vị");
        ward1.setDistrictId(districtId);
        wards.add(ward1);
        
        GhnWardDto ward2 = new GhnWardDto();
        ward2.setWardCode("21013");
        ward2.setWardName("Phường Điện Biên");
        ward2.setDistrictId(districtId);
        wards.add(ward2);
        
        return wards;
    }
    
    @Override
    public int getShippingFee(int toDistrictId, String toWardCode) {
        log.info("Mock: Calculating shipping fee for district {} ward {}", toDistrictId, toWardCode);
        // Fixed shipping fee for testing
        return 25000; // 25k VND
    }
    
    @Override
    public int getShippingFeeWithInsurance(int toDistrictId, String toWardCode, int orderValue) {
        log.info("Mock: Calculating shipping fee with insurance for district {} ward {}", toDistrictId, toWardCode);
        // Fixed shipping fee + small insurance
        return 27000; // 27k VND
    }
    
    @Override
    public GhnCalculateFeeResponse calculateShippingFeeSimple(ShippingFeeRequest request) {
        log.info("Mock: Calculating shipping fee simple");
        GhnCalculateFeeResponse response = new GhnCalculateFeeResponse();
        response.setTotal(25000);
        response.setServiceFee(23000);
        response.setInsuranceFee(2000);
        return response;
    }
}
