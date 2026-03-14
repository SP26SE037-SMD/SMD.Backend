package com.example.smd.controller;

import com.nimbusds.jose.JOSEException;
import com.example.smd.dto.request.auth.AuthenticationRequest;
import com.example.smd.dto.request.auth.LoginGoogleRequest;
import com.example.smd.dto.request.auth.ResetPasswordRequest;
import com.example.smd.dto.response.AuthenticationResponse;
import com.example.smd.dto.response.account.AccountResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@Tag(name = "Authentication", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // API đăng nhập để lấy access token
    @PostMapping("/login")
    @Operation(summary = "Login to get access token")
    public ResponseObject<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseObject.<AuthenticationResponse>builder()
                .status(1000)
                .data(authenticationService.authenticate(request))
                .message("Login successfully")
                .build();
    }

    // API đăng nhập với Google
    @PostMapping("/login-google")
    @Operation(summary = "Login with Google account")
    public ResponseObject<AuthenticationResponse> authenticateGoogle(@Valid @RequestBody LoginGoogleRequest request) {
        return ResponseObject.<AuthenticationResponse>builder()
                .status(1000)
                .data(authenticationService.authenticateGoogle(request))
                .message("Login with Google successfully")
                .build();
    }

    // API kiểm tra token có hợp lệ không
    @PostMapping("/introspect")
    @Operation(summary = "Validate token")
    public ResponseObject<Boolean> introspect(@RequestParam String token) throws ParseException, JOSEException {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(authenticationService.introspect(token))
                .message("Token is valid")
                .build();
    }

    // API đặt lại mật khẩu với token
    @PostMapping("/password-reset")
    @Operation(summary = "Reset password with token")
    public ResponseObject<Boolean> resetPassword(@Valid @RequestBody ResetPasswordRequest request)
            throws ParseException, JOSEException {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .message("Password reset successfully")
                .data(authenticationService.resetPassword(request))
                .build();
    }

    // API lấy thông tin tài khoản hiện tại bằng token
    @GetMapping("/me")
    @Operation(summary = "Get current account information by token")
    public ResponseObject<AccountResponse> getAccountByToken(@RequestParam String token)
            throws ParseException, JOSEException {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(authenticationService.getAccountByToken(token))
                .message("Get account information successfully")
                .build();
    }
}
