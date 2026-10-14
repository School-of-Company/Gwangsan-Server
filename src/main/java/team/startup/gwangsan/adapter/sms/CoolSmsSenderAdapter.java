package team.startup.gwangsan.adapter.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.domain.sms.exception.SmsSendFailedException;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;

/** coolsms(nurigo) SDK 발송. SDK 타입과 발신 번호는 이 adapter 안에만 둔다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoolSmsSenderAdapter implements SmsSenderPort {

    private final DefaultMessageService messageService;
    private final SmsProperties smsProperties;

    @Async("asyncExecutor")
    @Override
    public void sendAsync(String to, String text) {
        String maskedPhoneNumber = SmsAuthPolicy.mask(to);
        try {
            sendOne(to, text);
            log.info("[SMS] 비동기 발송 성공 - phoneNumber={}", maskedPhoneNumber);
        } catch (Exception e) {
            log.error("[SMS] 비동기 발송 실패 - phoneNumber={}", maskedPhoneNumber, e);
        }
    }

    @Override
    public void send(String to, String text) {
        try {
            sendOne(to, text);
        } catch (Exception e) {
            log.error("[SMS] 발송 실패 - phoneNumber={}", SmsAuthPolicy.mask(to), e);
            throw new SmsSendFailedException();
        }
    }

    private void sendOne(String to, String text) throws Exception {
        Message message = new Message();
        message.setFrom(smsProperties.getFromNumber());
        message.setTo(to);
        message.setText(text);
        messageService.sendOne(new SingleMessageSendingRequest(message));
    }
}
