package team.startup.gwangsan.application.sms.port.in;

/** 가입·비밀번호 재설정·별칭 찾기 인증번호 확인. 세 경로의 규칙이 같다. */
public interface VerifyCodeUseCase {

    void execute(String phoneNumber, String code);
}
