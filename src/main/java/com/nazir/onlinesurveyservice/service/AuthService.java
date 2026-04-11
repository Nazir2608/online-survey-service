package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.dto.request.*;
import com.nazir.onlinesurveyservice.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
