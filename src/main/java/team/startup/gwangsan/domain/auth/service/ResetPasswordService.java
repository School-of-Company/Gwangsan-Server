package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.request.ResetPasswordRequest;

public interface ResetPasswordService {
    void execute(ResetPasswordRequest request);
}
