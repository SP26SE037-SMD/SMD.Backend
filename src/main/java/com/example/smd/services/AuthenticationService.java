package com.example.smd.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.smd.dto.request.AuthenticationRequest;
import com.example.smd.dto.request.ResetPasswordRequest;
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

import java.nio.charset.StandardCharsets;
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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid password");
        }

        if (!account.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account is inactive");
        }

        String token = generateToken(account);

        // Update last login
        account.setLastLogin(Instant.now());
        accountRepository.save(account);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .account(accountMapper.toResponse(account))
                .build();
    }

    public void verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String generateToken(Account account) {
        String username = account.getUsername();
        UUID accountId = account.getAccountId();
        String scope = buildScope(account);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .claim("accountId", accountId)
                .claim("scope", scope)
                .jwtID(UUID.randomUUID().toString())
                .issuer("smd")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .build();

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

    public boolean introspect(String token) throws JOSEException, ParseException {
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return isValid;
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) throws ParseException, JOSEException {
        String token = request.getAccessToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // Validate token
        if (!introspect(token)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Validate password confirmation
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Parse token to get accountId
        SignedJWT signedJWT = SignedJWT.parse(token);
        String accountId =  signedJWT.getJWTClaimsSet().getStringClaim("accountId");
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Update password
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        return true;
    }

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
