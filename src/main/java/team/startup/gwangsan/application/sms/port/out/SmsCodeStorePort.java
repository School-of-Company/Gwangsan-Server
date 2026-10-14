package team.startup.gwangsan.application.sms.port.out;

import java.time.Duration;
import java.util.Optional;

public interface SmsCodeStorePort {

    Optional<Integer> findAttempt(String phoneNumber);

    void saveAttempt(String phoneNumber, int attempt, Duration ttl);

    void saveCode(String phoneNumber, String code, Duration ttl);

    Optional<String> findCode(String phoneNumber);

    void deleteCode(String phoneNumber);

    void saveVerified(String phoneNumber, Duration ttl);

    Optional<Boolean> findVerified(String phoneNumber);

    void deleteVerified(String phoneNumber);
}
