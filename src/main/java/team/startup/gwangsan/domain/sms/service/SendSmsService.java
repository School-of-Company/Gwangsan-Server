package team.startup.gwangsan.domain.sms.service;

import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;

public interface SendSmsService {
    void execute(SendSmsRequest dto);
}
