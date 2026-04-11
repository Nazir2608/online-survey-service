package com.nazir.onlinesurveyservice.service.impl;

import com.nazir.onlinesurveyservice.config.AppProperties;
import com.nazir.onlinesurveyservice.domain.entity.RefreshToken;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.dto.request.LoginRequest;
import com.nazir.onlinesurveyservice.dto.request.RefreshTokenRequest;
import com.nazir.onlinesurveyservice.dto.request.RegisterRequest;
import com.nazir.onlinesurveyservice.dto.response.AuthResponse;
import com.nazir.onlinesurveyservice.exception.DuplicateResourceException;
import com.nazir.onlinesurveyservice.exception.InvalidTokenException;
import com.nazir.onlinesurveyservice.repository.RefreshTokenRepository;
import com.nazir.onlinesurveyservice.repository.UserRepository;
import com.nazir.onlinesurveyservice.security.jwt.JwtTokenProvider;
import com.nazir.onlinesurveyservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider      jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AppProperties         appProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Email already in use: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Role.RESPONDENT)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Revoke all existing refresh tokens for this user (single-session policy)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token has expired or been revoked");
        }

        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken        = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue  = jwtTokenProvider.generateRefreshTokenValue();

        long expirationMs = appProperties.getJwt().getRefreshTokenExpirationMs();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .build();
        refreshTokenRepository.save(refreshToken);

        long accessExpiresIn = appProperties.getJwt().getAccessTokenExpirationMs() / 1000;

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                AuthResponse.BEARER,
                accessExpiresIn,
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole())
        );
    }
}
