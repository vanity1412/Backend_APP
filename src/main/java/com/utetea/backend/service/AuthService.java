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
    private final OtpService otpService;
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getPhone());
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
        System.out.println("========== LOGIN SERVICE START ==========");
        System.out.println("Login attempt for: " + request.getUsernameOrPhone());
        
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
        
        System.out.println("User found - ID: " + user.getId());
        System.out.println("User found - Username: " + user.getUsername());
        System.out.println("User found - Role: " + user.getRole());
        System.out.println("User found - Role.name(): " + user.getRole().name());
        
        if (user.getIsBlocked()) {
            throw new BusinessException("Account is blocked");
        }
        
        if (!user.getActive()) {
            throw new BusinessException("Account is inactive");
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        
        LoginResponse response = mapToLoginResponse(user, token);
        System.out.println("Response created - Role: " + response.getRole());
        System.out.println("========== LOGIN SERVICE END ==========");
        
        return response;
    }

    @Transactional
    public void registerWithOtp(RegisterRequest request) {
        System.out.println("================== START REGISTER WITH OTP ==================");
        System.out.println("Request - Username: " + request.getUsername());
        System.out.println("Request - Phone: " + request.getPhone());
        System.out.println("Request - Email: " + request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            System.out.println("ERROR: Username already exists");
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            System.out.println("ERROR: Phone already exists");
            throw new BusinessException("Phone already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userRepository.existsByEmail(request.getEmail())) {
            System.out.println("ERROR: Email already exists");
            throw new BusinessException("Email already exists");
        }

        System.out.println("Validation passed, creating user...");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getPhone());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.USER);
        user.setMemberTier(MemberTier.BRONZE);
        user.setPoints(0);
        user.setActive(false);
        user.setIsBlocked(false);
        // --- LOGIC MỚI: TẠO OTP VÀ GÁN LUÔN TRƯỚC KHI SAVE ---
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Set thời hạn 5 phút

        System.out.println("User object created, saving to database...");
        user = userRepository.save(user);
        System.out.println("User saved! User ID: " + user.getId());
        System.out.println("User in DB - Username: " + user.getUsername());
        System.out.println("User in DB - Phone: " + user.getPhone());
        System.out.println("User in DB - Email: " + user.getEmail());
        System.out.println("User in DB - Active: " + user.getActive());
        System.out.println("User in DB - OTP before send: " + user.getOtp());

        String email = request.getPhone();
        if (email == null || email.isEmpty()) {
            System.out.println("ERROR: Email is null or empty");
            throw new BusinessException("Email is required for OTP registration");
        }

        System.out.println("Calling otpService.sendOtp()...");
        try {
            otpService.sendOtp(otp, email);
            System.out.println("otpService.sendOtp() completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR in otpService.sendOtp(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        System.out.println("================== END REGISTER WITH OTP ==================");
    }
    
    @Transactional
    public LoginResponse verifyOtpAndActivate(String phone, String otp) {
        // Verify OTP
        if (!otpService.verifyOtp(phone, otp)) {
            throw new BusinessException("Invalid or expired OTP");
        }
        
        // Find user by phone
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Activate user
        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user = userRepository.save(user);
        
        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        
        return mapToLoginResponse(user, token);
    }
    
    @Transactional(readOnly = true)
    public void resendOtp(String phoneOrEmail) {
        User user = null;
        String email = null;
        String phone = null;
        String otp = otpService.generateOtp();

        if (phoneOrEmail != null && phoneOrEmail.contains("@")) {
            user = userRepository.findByEmail(phoneOrEmail).orElse(null);
            email = phoneOrEmail;
            if (user != null) phone = user.getPhone();
        } else {
            user = userRepository.findByPhone(phoneOrEmail).orElse(null);
            if (user != null) {
                phone = user.getPhone();
                email = user.getPhone();
                user.setOtp(otp);
                user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Set thời hạn 5 phút

            }
        }
        if (user == null) {
            throw new BusinessException("User not found");
        }
        if (email == null || email.isEmpty()) {
            throw new BusinessException("Email is not set for this user");
        }
        otpService.sendOtp(otp, email);
    }

    @Transactional
    public void verifyOtpAndActivateByEmail(String email, String otp) {
        // 1. Check OTP
        if (!otpService.verifyOtpByEmail(email, otp)) {
            throw new BusinessException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        // 2. Lấy User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        // 3. Kích hoạt User
        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);

        // 4. Lưu lại
        userRepository.save(user);

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
        
        System.out.println("========== MAP TO LOGIN RESPONSE ==========");
        System.out.println("User Role (enum): " + user.getRole());
        System.out.println("User Role (name): " + user.getRole().name());
        System.out.println("Response Role: " + response.getRole());
        System.out.println("Response Role (name): " + response.getRole().name());
        System.out.println("==========================================");
        
        return response;
    }
}
