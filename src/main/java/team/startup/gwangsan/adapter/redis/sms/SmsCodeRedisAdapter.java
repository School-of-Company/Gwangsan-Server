package team.startup.gwangsan.adapter.redis.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.adapter.redis.RedisUtil;

import java.time.Duration;
import java.util.Optional;

/** SMS 인증 Redis 키. 키 이름·값 타입(JSON)·TTL 은 기존 저장 계약 그대로다. */
@Component
@RequiredArgsConstructor
public class SmsCodeRedisAdapter implements SmsCodeStorePort {

    private static final String ATTEMPT_KEY_PREFIX = "sms:attempt:";
    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private final RedisUtil redisUtil;

    @Override
    public Optional<Integer> findAttempt(String phoneNumber) {
        return Optional.ofNullable(redisUtil.get(ATTEMPT_KEY_PREFIX + phoneNumber, Integer.class));
    }

    @Override
    public void saveAttempt(String phoneNumber, int attempt, Duration ttl) {
        redisUtil.set(ATTEMPT_KEY_PREFIX + phoneNumber, attempt, ttl.toMillis());
    }

    @Override
    public void saveCode(String phoneNumber, String code, Duration ttl) {
        redisUtil.set(CODE_KEY_PREFIX + phoneNumber, code, ttl.toMillis());
    }

    @Override
    public Optional<String> findCode(String phoneNumber) {
        return Optional.ofNullable(redisUtil.get(CODE_KEY_PREFIX + phoneNumber, String.class));
    }

    @Override
    public void deleteCode(String phoneNumber) {
        redisUtil.delete(CODE_KEY_PREFIX + phoneNumber);
    }

    @Override
    public void saveVerified(String phoneNumber, Duration ttl) {
        redisUtil.set(VERIFIED_KEY_PREFIX + phoneNumber, Boolean.TRUE, ttl.toMillis());
    }

    @Override
    public Optional<Boolean> findVerified(String phoneNumber) {
        return Optional.ofNullable(redisUtil.get(VERIFIED_KEY_PREFIX + phoneNumber, Boolean.class));
    }

    @Override
    public void deleteVerified(String phoneNumber) {
        redisUtil.delete(VERIFIED_KEY_PREFIX + phoneNumber);
    }
}
