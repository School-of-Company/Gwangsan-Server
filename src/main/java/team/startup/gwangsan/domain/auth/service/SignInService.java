package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;

public interface SignInService {
    TokenResponse execute(SignInRequest request);
}
