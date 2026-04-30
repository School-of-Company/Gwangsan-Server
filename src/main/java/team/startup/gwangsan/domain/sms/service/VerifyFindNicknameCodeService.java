package team.startup.gwangsan.domain.sms.service;

import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;

public interface VerifyFindNicknameCodeService {
    void execute(VerifyCodeRequest request);
}
