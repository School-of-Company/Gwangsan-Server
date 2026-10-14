package team.startup.gwangsan.application.auth.port.out;

/** SMS 인증 결과. 인증 데이터는 sms 업무가 소유한다. */
public interface PhoneVerificationPort {

    boolean isDemoNumber(String phoneNumber);

    /** 인증 완료 표시. 요청 기록이 없으면 null. */
    Boolean findVerified(String phoneNumber);

    void consume(String phoneNumber);
}
