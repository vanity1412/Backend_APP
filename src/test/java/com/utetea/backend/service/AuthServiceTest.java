package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho AuthService
 * FIX High #6: Thêm test coverage cho AuthService
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private UserDetailsService userDetailsService;
    
    @Mock
    private OtpService otpService;
    
    @Mock
    private UserDetails mockUserDetails;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("0909123456");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setMemberTier(MemberTier.BRONZE);
        testUser.setActive(true);
        testUser.setIsBlocked(false);
    }
    
    // ==================== Login Tests ====================
    
    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrPhone("testuser");
        request.setPassword("password123");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null);
        when(userRepository.findByUsernameOrPhone("testuser", "testuser"))
            .thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("testuser"))
            .thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any(UserDetails.class), anyString()))
            .thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class)))
            .thenReturn("refresh_token");

        // Act
        LoginResponse result = authService.login(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("access_token", result.getToken());
        assertEquals("refresh_token", result.getRefreshToken());
    }
    
    @Test
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrPhone("testuser");
        request.setPassword("wrongpassword");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }
    
    @Test
    void login_UserNotFound_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrPhone("nonexistent");
        request.setPassword("password123");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null);
        when(userRepository.findByUsernameOrPhone("nonexistent", "nonexistent"))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.login(request);
        });
    }
    
    @Test
    void login_BlockedUser_ThrowsException() {
        // Arrange
        testUser.setIsBlocked(true);
        LoginRequest request = new LoginRequest();
        request.setUsernameOrPhone("testuser");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null);
        when(userRepository.findByUsernameOrPhone("testuser", "testuser"))
            .thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.login(request);
        });
    }
    
    @Test
    void login_InactiveUser_ThrowsException() {
        // Arrange
        testUser.setActive(false);
        LoginRequest request = new LoginRequest();
        request.setUsernameOrPhone("testuser");
        request.setPassword("password123");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null);
        when(userRepository.findByUsernameOrPhone("testuser", "testuser"))
            .thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.login(request);
        });
    }
    
    // ==================== RegisterWithOtp Tests ====================
    
    @Test
    void registerWithOtp_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFullName("New User");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(otpService.generateOtp()).thenReturn("123456");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        assertDoesNotThrow(() -> authService.registerWithOtp(request));
        
        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(otpService, times(1)).sendOtp("123456", "new@example.com");
    }

    @Test
    void registerWithOtp_DuplicateUsername_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.registerWithOtp(request);
        });
    }
    
    @Test
    void registerWithOtp_DuplicateEmail_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.registerWithOtp(request);
        });
    }
    
    @Test
    void registerWithOtp_EmptyEmail_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("");
        request.setPassword("password123");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(otpService.generateOtp()).thenReturn("123456");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.registerWithOtp(request);
        });
    }
    
    // ==================== VerifyOtpAndActivate Tests ====================
    
    @Test
    void verifyOtpAndActivate_Success() {
        // Arrange
        when(otpService.verifyOtp("0909123456", "123456")).thenReturn(true);
        when(userRepository.findByPhone("0909123456")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any(UserDetails.class), anyString())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");
        
        // Act
        LoginResponse result = authService.verifyOtpAndActivate("0909123456", "123456");
        
        // Assert
        assertNotNull(result);
        assertTrue(testUser.getActive());
        assertNull(testUser.getOtp());
    }

    @Test
    void verifyOtpAndActivate_InvalidOtp_ThrowsException() {
        // Arrange
        when(otpService.verifyOtp("0909123456", "000000")).thenReturn(false);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.verifyOtpAndActivate("0909123456", "000000");
        });
    }
    
    @Test
    void verifyOtpAndActivate_UserNotFound_ThrowsException() {
        // Arrange
        when(otpService.verifyOtp("0909123456", "123456")).thenReturn(true);
        when(userRepository.findByPhone("0909123456")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.verifyOtpAndActivate("0909123456", "123456");
        });
    }
    
    // ==================== RefreshAccessToken Tests ====================
    
    @Test
    void refreshAccessToken_Success() {
        // Arrange
        String oldRefreshToken = "old_refresh_token";
        
        when(jwtUtil.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.extractUsernameFromRefreshToken(oldRefreshToken)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UserDetails.class), anyString())).thenReturn("new_access");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("new_refresh");
        when(jwtUtil.getAccessTokenExpirationTime()).thenReturn(86400000L);
        
        // Act
        JwtResponse result = authService.refreshAccessToken(oldRefreshToken);
        
        // Assert
        assertNotNull(result);
        assertEquals("new_access", result.getToken());
        assertEquals("new_refresh", result.getRefreshToken());
    }
    
    @Test
    void refreshAccessToken_InvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "invalid_token";
        
        when(jwtUtil.validateRefreshToken(invalidToken)).thenReturn(false);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.refreshAccessToken(invalidToken);
        });
    }
    
    @Test
    void refreshAccessToken_UserNotFound_ThrowsException() {
        // Arrange
        String refreshToken = "valid_refresh_token";
        
        when(jwtUtil.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsernameFromRefreshToken(refreshToken)).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.refreshAccessToken(refreshToken);
        });
    }
    
    @Test
    void refreshAccessToken_InactiveUser_ThrowsException() {
        // Arrange
        testUser.setActive(false);
        String refreshToken = "valid_refresh_token";
        
        when(jwtUtil.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsernameFromRefreshToken(refreshToken)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.refreshAccessToken(refreshToken);
        });
    }
}
