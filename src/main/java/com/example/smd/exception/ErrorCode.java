package com.example.smd.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // Authentication & Authorization
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1003, "Invalid username or password", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1004, "Password and confirm password do not match", HttpStatus.BAD_REQUEST),

    // Account
    ACCOUNT_NOT_FOUND(2001, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_EXISTS(2002, "Account already exists", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTS(2003, "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS(2004, "Email already exists", HttpStatus.BAD_REQUEST),

    // Role
    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_EXISTS(3002, "Role already exists", HttpStatus.BAD_REQUEST),

    // Permission
    PERMISSION_NOT_FOUND(4001, "Permission not found", HttpStatus.NOT_FOUND),
    PERMISSION_EXISTS(4002, "Permission already exists", HttpStatus.BAD_REQUEST),

    // Major
    MAJOR_NOT_FOUND(5001, "Major not found", HttpStatus.NOT_FOUND),
    MAJOR_CODE_EXISTS(5002, "Major code already exists", HttpStatus.BAD_REQUEST),
    MAJOR_CODE_REQUIRED(5003, "Major code is required", HttpStatus.BAD_REQUEST),
    MAJOR_NAME_REQUIRED(5004, "Major name is required", HttpStatus.BAD_REQUEST),
    MAJOR_CODE_TOO_LONG(5005, "Major code must not exceed 20 characters", HttpStatus.BAD_REQUEST),

    // General
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1000, "Invalid message key", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
