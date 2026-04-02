package team.startup.gwangsan.domain.sms.service;

import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;

public interface SendResetPasswordSmsService {
    void execute(SendSmsRequest request);
}
