package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.AuthCodeGenerationException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.service.SendSmsService;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.sms.SmsProperties;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SendSmsServiceImpl implements SendSmsService {

    private final DefaultMessageService messageService;
    private final SmsProperties smsProperties;
    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;

    @Override
    public SingleMessageSentResponse execute(SendSmsRequest request) {

        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new AlreadyRegisteredPhoneNumberException();
        }

        String code = generateCode();

        saveAuthInfo(request.phoneNumber(), code);

        Message message = new Message();
        message.setFrom(smsProperties.getFromNumber());
        message.setTo(request.phoneNumber());
        message.setText("[시민화폐광산] 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");

        return messageService.sendOne(new SingleMessageSendingRequest(message));
    }

    private String generateCode() {
        return "000000";
    }

    private void saveAuthInfo(String phoneNumber, String code) {

        String attemptKey = "sms:attempt:" + phoneNumber;
        String codeKey = "sms:code:" + phoneNumber;

        Integer attempt = redisUtil.get(attemptKey, Integer.class);
        if (attempt == null) attempt = 0;

        if (attempt >= 5) throw new TooManyRequestAuthCodeException();

        redisUtil.set(attemptKey, attempt + 1, 3 * 60 * 1000);

        redisUtil.set(codeKey, code, 3 * 60 * 1000);
    }
}
