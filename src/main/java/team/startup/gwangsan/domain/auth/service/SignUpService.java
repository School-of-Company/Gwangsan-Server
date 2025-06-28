package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.SignUpRequest;

public interface SignUpService {
    void execute(SignUpRequest request);
}
