package com.utetea.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * FIX High #5: Removed hardcoded default JWT secret
 * Secret MUST be configured via environment variable and be at least 256 bits (32 characters)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours (Access Token)
    private Long expiration;

    // THÊM THỜI GIAN HẾT HẠN CHO REFRESH TOKEN (Ví dụ: 7 ngày)
    @Value("${jwt.refresh.expiration:604800000}") // 7 days (Refresh Token)
    private Long refreshExpiration;

    @PostConstruct
    public void validateConfig() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured via jwt.secret property");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters (256 bits) for HS256");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            // Xử lý khi token không hợp lệ (hết hạn, sai chữ ký,...)
            throw new JwtException("Invalid JWT token: " + e.getMessage());
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Phương thức tạo Access Token
    public String generateToken(UserDetails userDetails, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername(), expiration); // Dùng expiration mặc định
    }

    // THÊM PHƯƠNG THỨC TẠO REFRESH TOKEN
    public String generateRefreshToken(UserDetails userDetails) {
        // Refresh Token không cần chứa role, chỉ cần subject và thời gian hết hạn dài hơn
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshExpiration);
    }

    // Phương thức tạo Token chung, nhận vào thời gian hết hạn
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // >>> THÊM PHƯƠNG THỨC XÁC THỰC REFRESH TOKEN (chỉ kiểm tra thời gian)
    public Boolean validateRefreshToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }
    // <<< END THÊM

    // >>> THÊM PHƯƠNG THỨC TRÍCH XUẤT USERNAME TỪ REFRESH TOKEN
    public String extractUsernameFromRefreshToken(String token) {
        return extractUsername(token); // Tái sử dụng phương thức extractUsername
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // >>> THÊM PHƯƠNG THỨC LẤY THỜI GIAN HẾT HẠN CỦA ACCESS TOKEN
    public Long getAccessTokenExpirationTime() {
        return expiration;
    }
}