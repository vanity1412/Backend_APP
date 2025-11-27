package com.utetea.backend.service;

import com.utetea.backend.dto.LoginRequest;
import com.utetea.backend.dto.LoginResponse;
import com.utetea.backend.dto.RegisterRequest;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.USER);
        user.setMemberTier(MemberTier.BRONZE);
        user.setPoints(0);
        user.setActive(true);
        user.setIsBlocked(false);
        
        user = userRepository.save(user);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        
        return mapToLoginResponse(user, token);
    }
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Authenticate with Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrPhone(),
                        request.getPassword()
                )
        );
        
        User user = userRepository.findByUsernameOrPhone(
            request.getUsernameOrPhone(), 
            request.getUsernameOrPhone()
        ).orElseThrow(() -> new BusinessException("Invalid credentials"));
        
        if (user.getIsBlocked()) {
            throw new BusinessException("Account is blocked");
        }
        
        if (!user.getActive()) {
            throw new BusinessException("Account is inactive");
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        
        return mapToLoginResponse(user, token);
    }
    
    private LoginResponse mapToLoginResponse(User user, String token) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setPhone(user.getPhone());
        response.setFullName(user.getFullName());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setMemberTier(user.getMemberTier());
        response.setToken(token);
        return response;
    }
}
