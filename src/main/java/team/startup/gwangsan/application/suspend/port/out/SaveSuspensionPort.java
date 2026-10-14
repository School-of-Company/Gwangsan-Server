package team.startup.gwangsan.application.suspend.port.out;

import team.startup.gwangsan.domain.suspend.model.Suspension;

public interface SaveSuspensionPort {

    /** @return 저장된 정지 ID */
    Long save(Suspension suspension);

    /** 회원의 정지 행을 bulk 삭제한다(영속성 컨텍스트 자동 clear). */
    void deleteAllOf(Long memberId);
}
