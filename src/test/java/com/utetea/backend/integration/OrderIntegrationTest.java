package com.utetea.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetea.backend.dto.OrderItemRequest;
import com.utetea.backend.dto.OrderRequest;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private DrinkRepository drinkRepository;
    
    @Autowired
    private DrinkSizeRepository drinkSizeRepository;
    
    @Autowired
    private DrinkToppingRepository drinkToppingRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private Store testStore;
    private Drink testDrink;
    private DrinkSize testSize;
    private DrinkTopping testTopping;
    private Promotion testPromotion;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPhone("0909123456");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setFullName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
        
        // Create test store
        testStore = new Store();
        testStore.setStoreName("Test Store");
        testStore.setAddress("Test Address");
        testStore.setPhone("0901234567");
        testStore = storeRepository.save(testStore);
        
        // Create test drink
        testDrink = new Drink();
        testDrink.setName("Test Drink");
        testDrink.setDescription("Test Description");
        testDrink.setBasePrice(new BigDecimal("30000"));
        testDrink.setIsActive(true);
        testDrink = drinkRepository.save(testDrink);
        
        // Create test size
        testSize = new DrinkSize();
        testSize.setDrink(testDrink);
        testSize.setSizeName("M");
        testSize.setExtraPrice(BigDecimal.ZERO);
        testSize = drinkSizeRepository.save(testSize);
        
        // Create test topping
        testTopping = new DrinkTopping();
        testTopping.setDrink(testDrink);
        testTopping.setToppingName("Pearl");
        testTopping.setPrice(new BigDecimal("7000"));
        testTopping.setIsActive(true);
        testTopping = drinkToppingRepository.save(testTopping);
        
        // Create test promotion
        testPromotion = new Promotion();
        testPromotion.setCode("TEST20");
        testPromotion.setDescription("Test Promotion");
        testPromotion.setDiscountType(DiscountType.PERCENT);
        testPromotion.setDiscountValue(new BigDecimal("20"));
        testPromotion.setMinOrderValue(new BigDecimal("25000"));
        testPromotion.setStartDate(LocalDateTime.now().minusDays(1));
        testPromotion.setEndDate(LocalDateTime.now().plusDays(30));
        testPromotion.setIsActive(true);
        testPromotion = promotionRepository.save(testPromotion);
    }
    
    @Test
    void createOrder_Success() throws Exception {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("User-Id", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").exists())
                .andExpect(jsonPath("$.data.finalPrice").exists());
    }
    
    @Test
    void createOrder_WithPromotion_Success() throws Exception {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.setPromotionCode("TEST20");
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("User-Id", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionCode").value("TEST20"))
                .andExpect(jsonPath("$.data.discount").exists());
    }
    
    @Test
    void createOrder_WithToppings_Success() throws Exception {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setToppingIds(List.of(testTopping.getId()));
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("User-Id", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].toppings").isArray());
    }
    
    @Test
    void createOrder_InvalidUser_ReturnsBadRequest() throws Exception {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("User-Id", 99999L) // Non-existent user
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    void getUserOrders_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void validatePromotion_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/promotions/validate")
                .param("code", "TEST20")
                .param("orderAmount", "30000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TEST20"));
    }
    
    @Test
    void validatePromotion_InvalidCode_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/promotions/validate")
                .param("code", "INVALID")
                .param("orderAmount", "30000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    void validatePromotion_BelowMinAmount_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/promotions/validate")
                .param("code", "TEST20")
                .param("orderAmount", "10000")) // Below min 25000
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    // Helper methods
    private OrderRequest createValidOrderRequest() {
        OrderRequest request = new OrderRequest();
        request.setStoreId(testStore.getId());
        request.setType(OrderType.DELIVERY);
        request.setAddress("Test Address");
        request.setPaymentMethod(PaymentMethod.COD);
        
        OrderItemRequest item = new OrderItemRequest();
        item.setDrinkId(testDrink.getId());
        item.setSizeName("M");
        item.setQuantity(1);
        item.setNote("Test note");
        item.setToppingIds(new ArrayList<>());
        
        request.setItems(List.of(item));
        return request;
    }
}
