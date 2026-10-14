package team.startup.gwangsan.application.sms.port.in;

import java.util.Optional;

/** SMS 인증 완료 여부 공개 조회. auth 의 가입·재설정·별칭 찾기가 사용한다. */
public interface PhoneVerificationQuery {

    boolean isDemoNumber(String phoneNumber);

    Optional<Boolean> findVerified(String phoneNumber);

    /** 인증 완료 표시를 지운다. 같은 인증으로 두 번 사용하지 못한다. */
    void consume(String phoneNumber);
}
