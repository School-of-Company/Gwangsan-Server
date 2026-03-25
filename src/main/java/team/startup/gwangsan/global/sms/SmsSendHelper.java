package team.startup.gwangsan.global.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSendHelper {

    private final DefaultMessageService messageService;

    @Async("asyncExecutor")
    public void sendAsync(String from, String to, String text) {
        String maskedPhoneNumber = to.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        try {
            Message message = new Message();
            message.setFrom(from);
            message.setTo(to);
            message.setText(text);
            messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("[SMS] 비동기 발송 성공 - phoneNumber={}", maskedPhoneNumber);
        } catch (Exception e) {
            log.error("[SMS] 비동기 발송 실패 - phoneNumber={}", maskedPhoneNumber, e);
        }
    }
}