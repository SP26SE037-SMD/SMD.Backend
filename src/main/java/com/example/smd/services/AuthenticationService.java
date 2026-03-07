package com.example.smd.services;

import com.example.smd.dto.request.auth.LoginGoogleRequest;
import com.example.smd.dto.request.auth.ResetPasswordRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.smd.dto.request.auth.AuthenticationRequest;
import com.example.smd.dto.response.AuthenticationResponse;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.entities.Account;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountMapper;
import com.example.smd.repositories.AccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    AccountRepository accountRepository;
    AccountMapper accountMapper;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signer-key}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;


    // Xác thực đăng nhập và tạo token
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. Tìm tài khoản theo email
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));

        // 2. Kiểm tra password có khớp với password hash không
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid password");
        }

        // 3. Kiểm tra tài khoản có đang active không
        if (!account.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account is inactive");
        }

        // 4. Tạo token JWT
        String token = generateToken(account);

        // 5. Cập nhật thời gian đăng nhập cuối cùng
        account.setLastLogin(Instant.now());
        accountRepository.save(account);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .account(accountMapper.toResponse(account))
                .build();
    }

    // Xác thực đăng nhập Google và tạo token
    public AuthenticationResponse authenticateGoogle(LoginGoogleRequest request) {

        // 1. Verify token
        GoogleIdToken.Payload payload = null;
        try {
            payload = verifyGoogleToken(request.getIdToken());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // 2. Tìm account theo email
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(()-> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found with email: " + email));

        if (!account.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account inactive");
        }

        // 3. Tạo JWT của bạn
        String token = generateToken(account);

        account.setLastLogin(Instant.now());
        accountRepository.save(account);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .account(accountMapper.toResponse(account))
                .build();
    }

    public GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws GeneralSecurityException, IOException {

        GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);

        if (idToken == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid Google token");
        }

        return idToken.getPayload();
    }


    // Xác thực token JWT
    public void verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        // 1. Tạo verifier với SIGNER_KEY
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        // 2. Parse token thành SignedJWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 3. Kiểm tra thời gian hết hạn của token
        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        // 4. Xác minh chữ ký và kiểm tra token có hết hạn hay không
        var verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    // Tạo JWT token từ thông tin account
    private String generateToken(Account account) {
        // 1. Lấy thông tin cơ bản
        String email = account.getEmail();
        UUID accountId = account.getAccountId();
        String scope = buildScope(account);

        // 2. Tạo JWT claims set chứa thông tin user
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(email)
                .claim("accountId", accountId)
                .claim("scope", scope)
                .jwtID(UUID.randomUUID().toString())
                .issuer("smd")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .build();

        // 3. Ký token với HS512 và SIGNER_KEY
        try {
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claims
            );
            signedJWT.sign(new MACSigner(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Token generation failed", e);
            throw new RuntimeException(e);
        }
    }

    // Xây dựng scope (permissions) từ role của account
    private String buildScope(Account account) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (account.getRole() != null) {
            // Add all permissions from the role
            if (!CollectionUtils.isEmpty(account.getRole().getPermissions())) {
                account.getRole().getPermissions().forEach(permission ->
                    stringJoiner.add(permission.getPermissionName())
                );
            }
        }
        return stringJoiner.toString();
    }

    // Kiểm tra token có hợp lệ không (introspect)
    public boolean introspect(String token) throws JOSEException, ParseException {
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return isValid;
    }

    // Đặt lại mật khẩu
    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) throws ParseException, JOSEException {
        String token = request.getAccessToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. Xác thực token
        if (!introspect(token)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. Kiểm tra password có khớp với confirm password không
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. Lấy accountId từ token để tìm account
        SignedJWT signedJWT = SignedJWT.parse(token);
        String accountId =  signedJWT.getJWTClaimsSet().getStringClaim("accountId");
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 4. Cập nhật password mới
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        return true;
    }

    // Lấy thông tin account từ token
    public AccountResponse getAccountByToken(String token) throws ParseException, JOSEException {
        if (!introspect(token)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        SignedJWT signedJWT = SignedJWT.parse(token);
        String accountId = signedJWT.getJWTClaimsSet().getStringClaim("accountId");
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        return accountMapper.toResponse(account);
    }
}
