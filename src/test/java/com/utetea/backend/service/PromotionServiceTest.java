package com.utetea.backend.service;

import com.utetea.backend.dto.CreatePromotionRequest;
import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.dto.UpdatePromotionRequest;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.mapper.PromotionMapper;
import com.utetea.backend.model.DiscountType;
import com.utetea.backend.model.Promotion;
import com.utetea.backend.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho PromotionService
 * FIX High #7: Thêm test coverage cho PromotionService
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {
    
    @Mock
    private PromotionRepository promotionRepository;
    
    @Mock
    private PromotionMapper promotionMapper;
    
    @InjectMocks
    private PromotionService promotionService;
    
    private Promotion testPromotion;
    private PromotionDto testPromotionDto;

    @BeforeEach
    void setUp() {
        testPromotion = Promotion.builder()
            .code("TEST20")
            .description("Test Promotion")
            .discountType(DiscountType.PERCENT)
            .discountValue(new BigDecimal("20"))
            .startDate(LocalDateTime.of(2020, 1, 1, 0, 0))
            .endDate(LocalDateTime.of(2030, 12, 31, 23, 59))
            .minOrderValue(new BigDecimal("50000"))
            .minOrderAmount(BigDecimal.ZERO)
            .maxDiscountAmount(new BigDecimal("100000"))
            .usageLimit(100)
            .usedCount(0)
            .isActive(true)
            .build();
        testPromotion.setId(1L);
        
        testPromotionDto = new PromotionDto();
        testPromotionDto.setId(1L);
        testPromotionDto.setCode("TEST20");
        testPromotionDto.setDiscountType(DiscountType.PERCENT);
        testPromotionDto.setDiscountValue(new BigDecimal("20"));
    }
    
    // ==================== getAllActivePromotions Tests ====================
    
    @Test
    void getAllActivePromotions_Success() {
        // Arrange
        when(promotionRepository.findByIsActiveTrueAndEndDateAfter(any(LocalDateTime.class)))
            .thenReturn(List.of(testPromotion));
        when(promotionMapper.toDto(testPromotion)).thenReturn(testPromotionDto);
        
        // Act
        List<PromotionDto> result = promotionService.getAllActivePromotions();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TEST20", result.get(0).getCode());
    }
    
    @Test
    void getAllActivePromotions_Empty() {
        // Arrange
        when(promotionRepository.findByIsActiveTrueAndEndDateAfter(any(LocalDateTime.class)))
            .thenReturn(List.of());
        
        // Act
        List<PromotionDto> result = promotionService.getAllActivePromotions();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // ==================== getPromotionById Tests ====================
    
    @Test
    void getPromotionById_Success() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(promotionMapper.toDto(testPromotion)).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.getPromotionById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    void getPromotionById_NotFound_ThrowsException() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            promotionService.getPromotionById(1L);
        });
    }

    // ==================== validatePromotionCode Tests ====================
    
    @Test
    void validatePromotionCode_Success() {
        // Arrange
        when(promotionRepository.findByCodeAndIsActiveTrue("TEST20"))
            .thenReturn(Optional.of(testPromotion));
        when(promotionMapper.toDto(testPromotion)).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.validatePromotionCode("TEST20");
        
        // Assert
        assertNotNull(result);
        assertEquals("TEST20", result.getCode());
    }
    
    @Test
    void validatePromotionCode_InvalidCode_ThrowsException() {
        // Arrange
        when(promotionRepository.findByCodeAndIsActiveTrue("INVALID"))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.validatePromotionCode("INVALID");
        });
    }
    
    @Test
    void validatePromotionCode_NotStarted_ThrowsException() {
        // Arrange
        testPromotion.setStartDate(LocalDateTime.now().plusDays(1));
        when(promotionRepository.findByCodeAndIsActiveTrue("TEST20"))
            .thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.validatePromotionCode("TEST20");
        });
    }
    
    @Test
    void validatePromotionCode_Expired_ThrowsException() {
        // Arrange
        testPromotion.setEndDate(LocalDateTime.now().minusDays(1));
        when(promotionRepository.findByCodeAndIsActiveTrue("TEST20"))
            .thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.validatePromotionCode("TEST20");
        });
    }
    
    // ==================== validatePromotionWithAmount Tests ====================
    
    @Test
    void validatePromotionWithAmount_Success() {
        // Arrange
        when(promotionRepository.findByCodeAndIsActiveTrue("TEST20"))
            .thenReturn(Optional.of(testPromotion));
        when(promotionMapper.toDto(testPromotion)).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.validatePromotionWithAmount(
            "TEST20", new BigDecimal("100000"));
        
        // Assert
        assertNotNull(result);
    }
    
    @Test
    void validatePromotionWithAmount_BelowMinimum_ThrowsException() {
        // Arrange
        when(promotionRepository.findByCodeAndIsActiveTrue("TEST20"))
            .thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.validatePromotionWithAmount("TEST20", new BigDecimal("10000"));
        });
    }

    // ==================== createPromotion Tests ====================
    
    @Test
    void createPromotion_Success() {
        // Arrange
        CreatePromotionRequest request = new CreatePromotionRequest();
        request.setCode("NEW20");
        request.setDescription("New Promotion");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(new BigDecimal("20"));
        request.setStartDate(LocalDateTime.now());
        request.setEndDate(LocalDateTime.now().plusDays(30));
        request.setMinOrderValue(new BigDecimal("50000"));
        request.setIsActive(true);
        
        when(promotionRepository.findByCode("NEW20")).thenReturn(Optional.empty());
        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);
        when(promotionMapper.toDto(any(Promotion.class))).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.createPromotion(request);
        
        // Assert
        assertNotNull(result);
        verify(promotionRepository, times(1)).save(any(Promotion.class));
    }
    
    @Test
    void createPromotion_DuplicateCode_ThrowsException() {
        // Arrange
        CreatePromotionRequest request = new CreatePromotionRequest();
        request.setCode("TEST20");
        
        when(promotionRepository.findByCode("TEST20")).thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.createPromotion(request);
        });
    }
    
    @Test
    void createPromotion_InvalidDates_ThrowsException() {
        // Arrange
        CreatePromotionRequest request = new CreatePromotionRequest();
        request.setCode("NEW20");
        request.setStartDate(LocalDateTime.now().plusDays(30));
        request.setEndDate(LocalDateTime.now()); // End before start
        
        when(promotionRepository.findByCode("NEW20")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.createPromotion(request);
        });
    }
    
    @Test
    void createPromotion_PercentOver100_ThrowsException() {
        // Arrange
        CreatePromotionRequest request = new CreatePromotionRequest();
        request.setCode("NEW20");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(new BigDecimal("150")); // Over 100%
        request.setStartDate(LocalDateTime.now());
        request.setEndDate(LocalDateTime.now().plusDays(30));
        
        when(promotionRepository.findByCode("NEW20")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            promotionService.createPromotion(request);
        });
    }
    
    // ==================== updatePromotion Tests ====================
    
    @Test
    void updatePromotion_Success() {
        // Arrange
        UpdatePromotionRequest request = new UpdatePromotionRequest();
        request.setDescription("Updated Description");
        
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);
        when(promotionMapper.toDto(any(Promotion.class))).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.updatePromotion(1L, request);
        
        // Assert
        assertNotNull(result);
        verify(promotionRepository, times(1)).save(any(Promotion.class));
    }
    
    @Test
    void updatePromotion_NotFound_ThrowsException() {
        // Arrange
        UpdatePromotionRequest request = new UpdatePromotionRequest();
        
        when(promotionRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            promotionService.updatePromotion(1L, request);
        });
    }

    // ==================== deletePromotion Tests ====================
    
    @Test
    void deletePromotion_Success() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        
        // Act
        promotionService.deletePromotion(1L);
        
        // Assert
        verify(promotionRepository, times(1)).delete(testPromotion);
    }
    
    @Test
    void deletePromotion_NotFound_ThrowsException() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            promotionService.deletePromotion(1L);
        });
    }
    
    // ==================== togglePromotionStatus Tests ====================
    
    @Test
    void togglePromotionStatus_Success() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);
        when(promotionMapper.toDto(any(Promotion.class))).thenReturn(testPromotionDto);
        
        // Act
        PromotionDto result = promotionService.togglePromotionStatus(1L);
        
        // Assert
        assertNotNull(result);
        verify(promotionRepository, times(1)).save(any(Promotion.class));
    }
    
    @Test
    void togglePromotionStatus_NotFound_ThrowsException() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            promotionService.togglePromotionStatus(1L);
        });
    }
}
