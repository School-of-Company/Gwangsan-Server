package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;

public interface SignInAdminService {
    TokenResponse execute(String phoneNumber, String password);
}
