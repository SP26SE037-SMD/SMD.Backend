package com.example.smd;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordHashTest {

    @Test
    public void testAdminPasswordHash() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String hashedPassword = "$2a$10$kCmOYiP8bHlBT3zhTqMFq.0fELNI9QN/47TzCwV1GOJuWaxaiqJzC";

        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);

        System.out.println("==============================================");
        System.out.println("BCrypt Hash Verification Test");
        System.out.println("==============================================");
        System.out.println("Raw Password:    " + rawPassword);
        System.out.println("Hashed Password: " + hashedPassword);
        System.out.println("Match Result:    " + matches);
        System.out.println("==============================================");

        assertTrue(matches, "Password 'admin123' should match the BCrypt hash");
    }

    @Test
    public void generateNewHash() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String newHash = passwordEncoder.encode(rawPassword);

        System.out.println("==============================================");
        System.out.println("Generate New BCrypt Hash");
        System.out.println("==============================================");
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("New Hash:     " + newHash);
        System.out.println("==============================================");
    }
}
