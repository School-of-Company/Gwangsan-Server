package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;

public interface SignUpService {
    void execute(SignUpRequest request);
}
