package com.example.smd.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendTestEmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String toEmail;

    @NotBlank(message = "Subject name is required")
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;
}
