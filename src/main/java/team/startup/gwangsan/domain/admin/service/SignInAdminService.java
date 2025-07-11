package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;

public interface SignInAdminService {
    SignInAdminResponse execute(String phoneNumber, String password);
}
