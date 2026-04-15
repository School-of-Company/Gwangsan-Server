package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.sms.exception.AuthCodeGenerationException;
import team.startup.gwangsan.domain.sms.exception.NotRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.SmsSendFailedException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.service.SendFindNicknameSmsService;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.sms.SmsProperties;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendFindNicknameSmsServiceImpl implements SendFindNicknameSmsService {

    private final DefaultMessageService messageService;
    private final SmsProperties smsProperties;
    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;

    @Override
    @Transactional
    public void execute(SendSmsRequest request) {

        if (!memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new NotRegisteredPhoneNumberException();
        }

        String code = generateCode();

        validateAttemptLimit(request.phoneNumber());

        sendSms(request.phoneNumber(), code);

        saveAuthInfo(request.phoneNumber(), code);
    }

    private void validateAttemptLimit(String phoneNumber) {
        String attemptKey = "sms:attempt:" + phoneNumber;

        Integer attempt = redisUtil.get(attemptKey, Integer.class);
        if (attempt == null) attempt = 0;

        if (attempt >= 5) throw new TooManyRequestAuthCodeException();

        redisUtil.set(attemptKey, attempt + 1, 3 * 60 * 1000);
    }

    private void sendSms(String phoneNumber, String code) {
        try {
            Message message = new Message();
            message.setFrom(smsProperties.getFromNumber());
            message.setTo(phoneNumber);
            message.setText("[시민화폐광산] 별칭 찾기 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");
            messageService.sendOne(new SingleMessageSendingRequest(message));
        } catch (Exception e) {
            log.error("[SMS] 발송 실패 - phoneNumber={}", phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"), e);
            throw new SmsSendFailedException();
        }
    }

    private String generateCode() {
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) builder.append(random.nextInt(10));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AuthCodeGenerationException();
        }
    }

    private void saveAuthInfo(String phoneNumber, String code) {
        String codeKey = "sms:code:" + phoneNumber;
        redisUtil.set(codeKey, code, 3 * 60 * 1000);
    }
}
