package com.example.smd.controller;

import com.example.smd.dto.request.SendTestEmailRequest ;
import com.example.smd.services.EmailService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-email")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EmailTestController {

    private final EmailService gmailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody SendTestEmailRequest request) {

        try {
            gmailService.sendWelcomeEmail(
                    request.getToEmail(),
                    request.getSubject()
            );

            return ResponseEntity.ok("Email sent successfully 🚀");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to send email: " + e.getMessage());
        }
    }
}