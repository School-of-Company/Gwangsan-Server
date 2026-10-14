package team.startup.gwangsan.domain.sms.policy;

import team.startup.gwangsan.domain.sms.exception.AuthCodeGenerationException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

/** SMS 인증번호 발급 규칙. 시도 횟수와 코드는 3분, 인증 완료 표시는 10분 유지한다. */
public final class SmsAuthPolicy {

    public static final int MAX_ATTEMPT = 5;
    public static final Duration CODE_TTL = Duration.ofMinutes(3);
    public static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final int CODE_LENGTH = 6;

    private SmsAuthPolicy() {
    }

    public static String generateCode() {
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) builder.append(random.nextInt(10));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AuthCodeGenerationException();
        }
    }

    /** 이번 요청을 포함한 시도 횟수를 반환한다. 이미 최대 횟수면 거절한다. */
    public static int nextAttempt(Integer attempt) {
        int current = attempt == null ? 0 : attempt;
        if (current >= MAX_ATTEMPT) throw new TooManyRequestAuthCodeException();
        return current + 1;
    }

    public static String mask(String phoneNumber) {
        return phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
