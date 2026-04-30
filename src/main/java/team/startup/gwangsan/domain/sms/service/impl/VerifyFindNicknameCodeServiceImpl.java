package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;
import team.startup.gwangsan.domain.sms.service.VerifyFindNicknameCodeService;
import team.startup.gwangsan.global.redis.RedisUtil;

@Service
@RequiredArgsConstructor
public class VerifyFindNicknameCodeServiceImpl implements VerifyFindNicknameCodeService {

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private static final long VERIFIED_TTL_MILLIS = 10 * 60 * 1000L;

    private final RedisUtil redisUtil;

    @Override
    @Transactional
    public void execute(VerifyCodeRequest request) {
        String phoneNumber = request.phoneNumber();
        String codeKey = CODE_KEY_PREFIX + phoneNumber;
        String verifiedKey = VERIFIED_KEY_PREFIX + phoneNumber;

        String savedCode = redisUtil.get(codeKey, String.class);
        if (savedCode == null) {
            throw new SmsAuthNotFoundException();
        }

        if (!savedCode.equals(request.code())) {
            throw new NotMatchRandomCodeException();
        }

        redisUtil.set(verifiedKey, Boolean.TRUE, VERIFIED_TTL_MILLIS);
        redisUtil.delete(codeKey);
    }
}
