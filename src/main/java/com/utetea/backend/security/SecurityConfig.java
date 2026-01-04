package com.utetea.backend.security;

import java.util.Arrays;
import java.util.List;

import com.utetea.backend.filter.BlockedIPFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BlockedIPFilter blockedIPFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI - Allow all Swagger paths
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        
                        // Root and static resources
                        .requestMatchers("/", "/index.html", "/error").permitAll()
                        
                        // Actuator health check (for Railway)
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // Public endpoints (Auth - including OTP)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers("/api/drinks/**").permitAll()
                        .requestMatchers("/api/stores/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/promotions/**").permitAll()
                        .requestMatchers("/api/chatbot/**").permitAll()
                        .requestMatchers("/api/weather/**").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        
                        // VNPAY endpoints
                        .requestMatchers("/api/vnpay/callback").permitAll()
                        .requestMatchers("/api/vnpay/test-config").permitAll()
                        .requestMatchers("/api/vnpay/test-payment-url").permitAll()
                        
                        // WebSocket endpoints
                        .requestMatchers("/ws/**").permitAll()
                        
                        // Manager test endpoint (for debugging)
                        .requestMatchers("/api/manager/test").permitAll()
                        
                        // Manager endpoints - ADMIN và MANAGER đều có quyền truy cập
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        
                        // 🛡️ User Monitoring endpoints - CHỈ ADMIN
                        .requestMatchers("/api/monitoring/**").hasRole("ADMIN")
                        
                        // 🚫 Blocked IP endpoints - CHỈ ADMIN
                        .requestMatchers("/api/blocked-ips/**").hasRole("ADMIN")
                        
                        // Admin only endpoints - quản lý cao nhất
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // User endpoints - tất cả roles đều có quyền
                        .requestMatchers("/api/orders/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/group-orders/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/group-orders/*/chat/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/me/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/vouchers/validate").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/cart/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/vnpay/create-payment").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/vnpay/create-payment-amount").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/vnpay/create-order-after-payment").hasAnyRole("USER", "MANAGER", "ADMIN")
                        
                        // Review endpoints - public for reading, authenticated for writing
                        .requestMatchers("/api/reviews/drink/**").permitAll()
                        .requestMatchers("/api/reviews/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        
                        // Live Chat endpoints
                        .requestMatchers("/api/chat/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/chat/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        
                        // Predictive Order endpoints
                        .requestMatchers("/api/predictive-order/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        
                        // Notification endpoints - User và Manager đều có thể xem thông báo của mình
                        .requestMatchers("/api/notifications/send").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/notifications/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        
                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(blockedIPFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
