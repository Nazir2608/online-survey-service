package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.config.AppProperties;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.dto.request.LoginRequest;
import com.nazir.onlinesurveyservice.dto.request.RegisterRequest;
import com.nazir.onlinesurveyservice.dto.response.AuthResponse;
import com.nazir.onlinesurveyservice.exception.DuplicateResourceException;
import com.nazir.onlinesurveyservice.repository.RefreshTokenRepository;
import com.nazir.onlinesurveyservice.repository.UserRepository;
import com.nazir.onlinesurveyservice.security.jwt.JwtTokenProvider;
import com.nazir.onlinesurveyservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceImplTest {

    @Mock UserRepository         userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock JwtTokenProvider       jwtTokenProvider;
    @Mock AuthenticationManager  authenticationManager;
    @Mock AppProperties          appProperties;
    @Mock AppProperties.JwtProperties jwtProps;

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(appProperties.getJwt()).thenReturn(jwtProps);
        when(jwtProps.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(jwtProps.getAccessTokenExpirationMs()).thenReturn(900_000L);
    }

    @Test
    @DisplayName("register() creates user and returns tokens when email is new")
    void register_success() {
        RegisterRequest request = new RegisterRequest(
                "nazir@example.com", "password123", "Nazir");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("nazir@example.com");
        assertThat(response.user().role()).isEqualTo(Role.RESPONDENT);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() throws DuplicateResourceException when email already exists")
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com", "password123", "Existing");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() authenticates user and returns tokens")
    void login_success() {
        LoginRequest request = new LoginRequest("nazir@example.com", "password123");

        User user = User.builder()
                .email("nazir@example.com")
                .password("hashed")
                .displayName("Nazir")
                .role(Role.RESPONDENT)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).revokeAllByUserId(any());
    }
}
