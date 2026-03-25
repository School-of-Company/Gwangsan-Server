package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.sms.exception.AuthCodeGenerationException;
import team.startup.gwangsan.domain.sms.exception.NotRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.service.SendResetPasswordSmsService;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.sms.SmsSendHelper;
import team.startup.gwangsan.global.sms.SmsProperties;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SendResetPasswordSmsServiceImpl implements SendResetPasswordSmsService {

    private final SmsSendHelper smsSendHelper;
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

        saveAuthInfo(request.phoneNumber(), code);

        smsSendHelper.sendAsync(
                smsProperties.getFromNumber(),
                request.phoneNumber(),
                "[시민화폐광산] 비밀번호 재설정 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요."
        );
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

        String attemptKey = "sms:attempt:" + phoneNumber;
        String codeKey = "sms:code:" + phoneNumber;

        Integer attempt = redisUtil.get(attemptKey, Integer.class);
        if (attempt == null) attempt = 0;

        if (attempt >= 5) throw new TooManyRequestAuthCodeException();

        redisUtil.set(attemptKey, attempt + 1, 3 * 60 * 1000);
        redisUtil.set(codeKey, code, 3 * 60 * 1000);
    }
}
