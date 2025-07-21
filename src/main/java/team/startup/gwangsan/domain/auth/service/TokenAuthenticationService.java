package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.response.MemberInfoResponse;

public interface TokenAuthenticationService {
    MemberInfoResponse execute();
}
