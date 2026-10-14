package team.startup.gwangsan.application.auth.port.out;

/**
 * 가입 신청 관리자 알림과 추천인 알림. 커밋 후 비동기로 저장된다(확정 정책).
 * 가입 트랜잭션과 원자 저장하지 않으므로 알림 저장 실패가 가입을 rollback 하지 않는다.
 */
public interface SignUpAlertPort {

    void notifySignUp(Long memberId, Long recommenderId);
}
