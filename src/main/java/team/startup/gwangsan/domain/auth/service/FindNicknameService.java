package team.startup.gwangsan.domain.auth.service;

import team.startup.gwangsan.domain.auth.presentation.dto.request.FindNicknameRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.FindNicknameResponse;

public interface FindNicknameService {
    FindNicknameResponse execute(FindNicknameRequest request);
}
