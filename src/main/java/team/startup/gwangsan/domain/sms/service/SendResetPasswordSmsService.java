package team.startup.gwangsan.domain.sms.service;

import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;

public interface SendResetPasswordSmsService {
    SingleMessageSentResponse execute(SendSmsRequest request);
}
