package com.utetea.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tạo BCrypt hash cho password
 * Chạy class này để lấy hash chính xác
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin";
        String hash = encoder.encode(password);
        
        System.out.println("==============================================");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("==============================================");
        System.out.println("\nSQL UPDATE:");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE id = 39;");
        System.out.println("==============================================");
        
        // Test verify
        boolean matches = encoder.matches(password, hash);
        System.out.println("\nVerification: " + (matches ? "✓ CORRECT" : "✗ WRONG"));
    }
}
