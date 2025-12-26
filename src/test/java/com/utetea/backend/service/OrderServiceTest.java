package com.utetea.backend.service;

import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.OrderItemRequest;
import com.utetea.backend.dto.OrderRequest;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private DrinkRepository drinkRepository;
    
    @Mock
    private DrinkSizeRepository drinkSizeRepository;
    
    @Mock
    private DrinkToppingRepository drinkToppingRepository;
    
    @Mock
    private PromotionRepository promotionRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    private User testUser;
    private Store testStore;
    private Drink testDrink;
    private DrinkSize testSize;
    private DrinkTopping testTopping;
    private Promotion testPromotion;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setActive(true);
        testUser.setRole(UserRole.USER);
        
        // Setup test store
        testStore = new Store();
        testStore.setId(1L);
        testStore.setStoreName("Test Store");
        testStore.setAddress("Test Address");
        
        // Setup test drink
        testDrink = new Drink();
        testDrink.setId(1L);
        testDrink.setName("Test Drink");
        testDrink.setBasePrice(new BigDecimal("30000"));
        testDrink.setIsActive(true);
        
        // Setup test size
        testSize = new DrinkSize();
        testSize.setId(1L);
        testSize.setDrink(testDrink);
        testSize.setSizeName("M");
        testSize.setExtraPrice(BigDecimal.ZERO);
        
        // Setup test topping
        testTopping = new DrinkTopping();
        testTopping.setId(1L);
        testTopping.setDrink(testDrink);
        testTopping.setToppingName("Pearl");
        testTopping.setPrice(new BigDecimal("7000"));
        testTopping.setIsActive(true);
        
        // Setup test promotion - FIX Low #20: Sử dụng fixed dates để tránh flaky test
        testPromotion = new Promotion();
        testPromotion.setId(1L);
        testPromotion.setCode("TEST20");
        testPromotion.setDiscountType(DiscountType.PERCENT);
        testPromotion.setDiscountValue(new BigDecimal("20"));
        testPromotion.setMinOrderValue(new BigDecimal("50000"));
        // Sử dụng khoảng thời gian rộng để test không bị flaky
        testPromotion.setStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        testPromotion.setEndDate(LocalDateTime.of(2030, 12, 31, 23, 59));
        testPromotion.setIsActive(true);
    }
    
    @Test
    void createOrder_Success() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        
        Order savedOrder = createMockOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // Act
        OrderDto result = orderService.createOrder("testuser", request);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }
    
    @Test
    void createOrder_UserNotFound_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_InactiveUser_ThrowsException() {
        // Arrange
        testUser.setActive(false);
        OrderRequest request = createValidOrderRequest();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_StoreNotFound_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }

    
    @Test
    void createOrder_EmptyItems_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.setItems(new ArrayList<>());
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_DrinkNotFound_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_InactiveDrink_ThrowsException() {
        // Arrange
        testDrink.setIsActive(false);
        OrderRequest request = createValidOrderRequest();
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_InvalidSize_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setSizeName("XL"); // Invalid size
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_WithValidPromotion_Success() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.setPromotionCode("TEST20");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        when(promotionRepository.findByCodeForUpdate("TEST20")).thenReturn(Optional.of(testPromotion));
        
        Order savedOrder = createMockOrder();
        savedOrder.setPromotion(testPromotion);
        savedOrder.setDiscount(new BigDecimal("6000")); // 20% of 30000
        savedOrder.setFinalPrice(new BigDecimal("24000"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // Act
        OrderDto result = orderService.createOrder("testuser", request);
        
        // Assert
        assertNotNull(result);
        assertEquals("TEST20", result.getPromotionCode());
        assertTrue(result.getDiscount().compareTo(BigDecimal.ZERO) > 0);
    }
    
    @Test
    void createOrder_InvalidPromotionCode_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.setPromotionCode("INVALID");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        when(promotionRepository.findByCodeForUpdate("INVALID")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_ExpiredPromotion_ThrowsException() {
        // Arrange - FIX Low #20: Sử dụng fixed past date
        testPromotion.setEndDate(LocalDateTime.of(2020, 1, 1, 0, 0)); // Đã hết hạn
        OrderRequest request = createValidOrderRequest();
        request.setPromotionCode("TEST20");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        when(promotionRepository.findByCodeForUpdate("TEST20")).thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_BelowMinOrderValue_ThrowsException() {
        // Arrange
        testPromotion.setMinOrderValue(new BigDecimal("100000")); // Higher than order value
        OrderRequest request = createValidOrderRequest();
        request.setPromotionCode("TEST20");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        when(promotionRepository.findByCodeForUpdate("TEST20")).thenReturn(Optional.of(testPromotion));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void updateOrderStatus_Success() {
        // Arrange
        Order order = createMockOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // Act
        OrderDto result = orderService.updateOrderStatus(1L, OrderStatus.MAKING);
        
        // Assert
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }
    
    @Test
    void updateOrderStatus_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.MAKING);
        });
    }
    
    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        // Arrange
        Order order = createMockOrder();
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // Act & Assert - PENDING cannot go directly to DONE
        assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.DONE);
        });
    }
    
    @Test
    void updateOrderStatus_CompletedOrder_ThrowsException() {
        // Arrange
        Order order = createMockOrder();
        order.setStatus(OrderStatus.DONE);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.CANCELED);
        });
    }
    
    @Test
    void getOrderById_Success() {
        // Arrange
        Order order = createMockOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // Act
        OrderDto result = orderService.getOrderById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    void getOrderById_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderById(1L);
        });
    }
    
    @Test
    void getUserOrders_Success() {
        // Arrange
        List<Order> orders = List.of(createMockOrder());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(orders);
        
        // Act
        List<OrderDto> result = orderService.getUserOrders(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void getUserCurrentOrders_Success() {
        // Arrange
        List<Order> orders = List.of(createMockOrder());
        when(orderRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(1L, OrderStatus.DONE))
            .thenReturn(orders);
        
        // Act
        List<OrderDto> result = orderService.getUserCurrentOrders(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    // FIX Medium #13: Thêm test cases cho topping validation với đúng method findByIdWithDrink
    @Test
    void createOrder_WithInactiveTopping_ThrowsException() {
        // Arrange
        testTopping.setIsActive(false);
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setToppingIds(List.of(1L));
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        // FIX: Sử dụng findByIdWithDrink thay vì findById
        when(drinkToppingRepository.findByIdWithDrink(1L)).thenReturn(Optional.of(testTopping));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_WithToppingNotBelongingToDrink_ThrowsException() {
        // Arrange
        Drink anotherDrink = new Drink();
        anotherDrink.setId(2L);
        anotherDrink.setName("Another Drink");
        testTopping.setDrink(anotherDrink); // Topping thuộc drink khác
        
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setToppingIds(List.of(1L));
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        // FIX: Sử dụng findByIdWithDrink thay vì findById
        when(drinkToppingRepository.findByIdWithDrink(1L)).thenReturn(Optional.of(testTopping));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    @Test
    void createOrder_WithToppingNotFound_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setToppingIds(List.of(999L)); // Non-existent topping
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findByDrinkId(1L)).thenReturn(List.of(testSize));
        // FIX: Sử dụng findByIdWithDrink thay vì findById
        when(drinkToppingRepository.findByIdWithDrink(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder("testuser", request);
        });
    }
    
    // FIX Critical #3: Thêm test cases cho quantity validation
    @Test
    void createOrder_WithZeroQuantity_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setQuantity(0);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
        assertTrue(exception.getMessage().contains("Số lượng phải lớn hơn 0"));
    }
    
    @Test
    void createOrder_WithNegativeQuantity_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setQuantity(-1);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
        assertTrue(exception.getMessage().contains("Số lượng phải lớn hơn 0"));
    }
    
    @Test
    void createOrder_WithQuantityExceedingMax_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setQuantity(101); // Exceeds max of 100
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
        assertTrue(exception.getMessage().contains("Số lượng tối đa là 100"));
    }
    
    @Test
    void createOrder_WithNullQuantity_ThrowsException() {
        // Arrange
        OrderRequest request = createValidOrderRequest();
        request.getItems().get(0).setQuantity(null);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder("testuser", request);
        });
        assertTrue(exception.getMessage().contains("Số lượng phải lớn hơn 0"));
    }
    
    // Helper methods
    private OrderRequest createValidOrderRequest() {
        OrderRequest request = new OrderRequest();
        request.setStoreId(1L);
        request.setType(OrderType.DELIVERY);
        request.setAddress("Test Address");
        request.setPaymentMethod(PaymentMethod.COD);
        
        OrderItemRequest item = new OrderItemRequest();
        item.setDrinkId(1L);
        item.setSizeName("M");
        item.setQuantity(1);
        item.setNote("Test note");
        item.setToppingIds(new ArrayList<>());
        
        request.setItems(List.of(item));
        return request;
    }
    
    private Order createMockOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStore(testStore);
        order.setType(OrderType.DELIVERY);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(new BigDecimal("30000"));
        order.setDiscount(BigDecimal.ZERO);
        order.setFinalPrice(new BigDecimal("30000"));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setItems(new ArrayList<>());
        return order;
    }
}
