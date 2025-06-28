package team.startup.gwangsan.domain.sms.service;

import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;

public interface SendSmsService {
    SingleMessageSentResponse execute(SendSmsRequest dto);
}
