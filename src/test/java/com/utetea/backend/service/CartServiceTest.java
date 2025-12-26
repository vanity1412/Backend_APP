package com.utetea.backend.service;

import com.utetea.backend.dto.AddToCartRequest;
import com.utetea.backend.dto.CartDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho CartService
 * FIX Critical #1: Thêm test coverage cho CartService
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    
    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private CartItemRepository cartItemRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DrinkRepository drinkRepository;
    
    @Mock
    private DrinkSizeRepository drinkSizeRepository;
    
    @Mock
    private DrinkToppingRepository drinkToppingRepository;
    
    @InjectMocks
    private CartService cartService;
    
    private User testUser;
    private Drink testDrink;
    private DrinkSize testSize;
    private DrinkTopping testTopping;
    private Cart testCart;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setActive(true);
        
        // Setup test drink
        testDrink = new Drink();
        testDrink.setId(1L);
        testDrink.setName("Test Drink");
        testDrink.setBasePrice(new BigDecimal("30000"));
        testDrink.setIsActive(true);
        testDrink.setImageUrl("/images/test.jpg");
        
        // Setup test size
        testSize = new DrinkSize();
        testSize.setId(1L);
        testSize.setDrink(testDrink);
        testSize.setSizeName("M");
        testSize.setExtraPrice(new BigDecimal("5000"));
        
        // Setup test topping
        testTopping = new DrinkTopping();
        testTopping.setId(1L);
        testTopping.setDrink(testDrink);
        testTopping.setToppingName("Pearl");
        testTopping.setPrice(new BigDecimal("7000"));
        testTopping.setIsActive(true);
        
        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
    }
    
    // ==================== addToCart Tests ====================
    
    @Test
    void addToCart_Success_NewCart() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findById(1L)).thenReturn(Optional.of(testSize));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        CartDto result = cartService.addToCart(1L, request);
        
        // Assert
        assertNotNull(result);
        verify(cartRepository, times(2)).save(any(Cart.class)); // Once for new cart, once implicit
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }
    
    @Test
    void addToCart_Success_ExistingCart() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findById(1L)).thenReturn(Optional.of(testSize));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        CartDto result = cartService.addToCart(1L, request);
        
        // Assert
        assertNotNull(result);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }
    
    @Test
    void addToCart_UserNotFound_ThrowsException() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart(1L, request);
        });
    }
    
    @Test
    void addToCart_DrinkNotFound_ThrowsException() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(drinkRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart(1L, request);
        });
    }
    
    @Test
    void addToCart_SizeNotFound_ThrowsException() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart(1L, request);
        });
    }
    
    @Test
    void addToCart_WithToppings_Success() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        request.setToppingIds(List.of(1L));
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(drinkSizeRepository.findById(1L)).thenReturn(Optional.of(testSize));
        when(drinkToppingRepository.findAllById(List.of(1L))).thenReturn(List.of(testTopping));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        CartDto result = cartService.addToCart(1L, request);
        
        // Assert
        assertNotNull(result);
        verify(drinkToppingRepository, times(1)).findAllById(List.of(1L));
    }
    
    @Test
    void addToCart_WithoutSize_Success() {
        // Arrange
        AddToCartRequest request = createValidAddToCartRequest();
        request.setSizeId(null);
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(drinkRepository.findById(1L)).thenReturn(Optional.of(testDrink));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        CartDto result = cartService.addToCart(1L, request);
        
        // Assert
        assertNotNull(result);
        verify(drinkSizeRepository, never()).findById(anyLong());
    }
    
    // ==================== getCart Tests ====================
    
    @Test
    void getCart_ExistingCart_Success() {
        // Arrange
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        
        // Act
        CartDto result = cartService.getCart(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
    }
    
    @Test
    void getCart_NewCart_CreatesAndReturns() {
        // Arrange
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        
        // Act
        CartDto result = cartService.getCart(1L);
        
        // Assert
        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
    
    @Test
    void getCart_UserNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.getCart(1L);
        });
    }
    
    // ==================== updateCartItemQuantity Tests ====================
    
    @Test
    void updateCartItemQuantity_Success() {
        // Arrange
        CartItem cartItem = createTestCartItem();
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        
        // Act
        CartDto result = cartService.updateCartItemQuantity(1L, 1L, 5);
        
        // Assert
        assertNotNull(result);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }
    
    @Test
    void updateCartItemQuantity_ZeroQuantity_DeletesItem() {
        // Arrange
        CartItem cartItem = createTestCartItem();
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        
        // Act
        CartDto result = cartService.updateCartItemQuantity(1L, 1L, 0);
        
        // Assert
        assertNotNull(result);
        verify(cartItemRepository, times(1)).delete(cartItem);
    }
    
    @Test
    void updateCartItemQuantity_NegativeQuantity_DeletesItem() {
        // Arrange
        CartItem cartItem = createTestCartItem();
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        
        // Act
        CartDto result = cartService.updateCartItemQuantity(1L, 1L, -1);
        
        // Assert
        assertNotNull(result);
        verify(cartItemRepository, times(1)).delete(cartItem);
    }
    
    @Test
    void updateCartItemQuantity_ItemNotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.updateCartItemQuantity(1L, 1L, 5);
        });
    }
    
    @Test
    void updateCartItemQuantity_WrongUser_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        
        Cart otherCart = new Cart();
        otherCart.setId(2L);
        otherCart.setUser(otherUser);
        
        CartItem cartItem = createTestCartItem();
        cartItem.setCart(otherCart);
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateCartItemQuantity(1L, 1L, 5);
        });
    }
    
    // ==================== removeCartItem Tests ====================
    
    @Test
    void removeCartItem_Success() {
        // Arrange
        CartItem cartItem = createTestCartItem();
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        
        // Act
        cartService.removeCartItem(1L, 1L);
        
        // Assert
        verify(cartItemRepository, times(1)).delete(cartItem);
    }
    
    @Test
    void removeCartItem_ItemNotFound_ThrowsException() {
        // Arrange
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeCartItem(1L, 1L);
        });
    }
    
    @Test
    void removeCartItem_WrongUser_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        
        Cart otherCart = new Cart();
        otherCart.setId(2L);
        otherCart.setUser(otherUser);
        
        CartItem cartItem = createTestCartItem();
        cartItem.setCart(otherCart);
        
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.removeCartItem(1L, 1L);
        });
    }
    
    // ==================== clearCart Tests ====================
    
    @Test
    void clearCart_Success() {
        // Arrange
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        
        // Act
        cartService.clearCart(1L);
        
        // Assert
        verify(cartItemRepository, times(1)).deleteByCartId(testCart.getId());
    }
    
    @Test
    void clearCart_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.clearCart(1L);
        });
    }
    
    // ==================== Helper Methods ====================
    
    private AddToCartRequest createValidAddToCartRequest() {
        AddToCartRequest request = new AddToCartRequest();
        request.setDrinkId(1L);
        request.setSizeId(1L);
        request.setQuantity(2);
        request.setNote("Test note");
        return request;
    }
    
    private CartItem createTestCartItem() {
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(testCart);
        cartItem.setDrink(testDrink);
        cartItem.setSize(testSize);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(35000.0);
        cartItem.setTotalPrice(70000.0);
        return cartItem;
    }
}
