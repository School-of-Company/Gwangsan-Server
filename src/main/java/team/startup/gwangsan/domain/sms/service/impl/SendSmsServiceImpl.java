package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;
import team.startup.gwangsan.domain.sms.exception.AuthCodeGenerationException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.repository.SmsAuthRepository;
import team.startup.gwangsan.domain.sms.service.SendSmsService;
import team.startup.gwangsan.global.sms.SmsProperties;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SendSmsServiceImpl implements SendSmsService {

    private final DefaultMessageService messageService;
    private final SmsProperties smsProperties;
    private final SmsAuthRepository smsAuthRepository;

    @Override
    public SingleMessageSentResponse execute(SendSmsRequest request) {
        String code = generateCode();

        Message message = new Message();
        message.setFrom(smsProperties.getFromNumber());
        message.setTo(request.phoneNumber());
        message.setText("[시민화폐광산] 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");

        saveAuthInfo(request.phoneNumber(), code);

        return messageService.sendOne(new SingleMessageSendingRequest(message));
    }

    private String generateCode() {
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AuthCodeGenerationException();
        }
    }

    private void saveAuthInfo(String phoneNumber, String code) {
        SmsAuthEntity entity = smsAuthRepository.findById(phoneNumber)
                .orElse(SmsAuthEntity.builder()
                        .phone(phoneNumber)
                        .authentication(false)
                        .attemptCount(0)
                        .randomValue(code)
                        .build());

        if (entity.getAttemptCount() >= 5) {
            throw new TooManyRequestAuthCodeException();
        }

        entity.plusAttemptCount();

        smsAuthRepository.save(SmsAuthEntity.builder()
                .phone(phoneNumber)
                .randomValue(code)
                .authentication(false)
                .attemptCount(entity.getAttemptCount())
                .build());
    }
}

