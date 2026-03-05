package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;
import team.startup.gwangsan.domain.sms.service.VerifyCodeService;
import team.startup.gwangsan.global.redis.RedisUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeServiceImpl implements VerifyCodeService {

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private static final long VERIFIED_TTL_MILLIS = 10 * 60 * 1000L;

    private static final String DEMO_PHONE_NUMBER = "01011111111";
    private static final String DEMO_FIXED_CODE = "000000";

    private final RedisUtil redisUtil;

    @Override
    @Transactional
    public void execute(VerifyCodeRequest request) {
        String phoneNumber = request.phoneNumber();
        String verifiedKey = VERIFIED_KEY_PREFIX + phoneNumber;

        if (DEMO_PHONE_NUMBER.equals(phoneNumber) && DEMO_FIXED_CODE.equals(request.code())) {
            redisUtil.set(verifiedKey, Boolean.TRUE, VERIFIED_TTL_MILLIS);
            log.info("[SMS] 인증 성공 (데모) - phoneNumber={}", phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            return;
        }

        String codeKey = CODE_KEY_PREFIX + phoneNumber;

        String savedCode = redisUtil.get(codeKey, String.class);
        if (savedCode == null) {
            throw new SmsAuthNotFoundException();
        }

        if (!savedCode.equals(request.code())) {
            throw new NotMatchRandomCodeException();
        }

        redisUtil.set(verifiedKey, Boolean.TRUE, VERIFIED_TTL_MILLIS);
        redisUtil.delete(codeKey);
        log.info("[SMS] 인증 성공 - phoneNumber={}", phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
    }
}
