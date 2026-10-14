package team.startup.gwangsan.application.suspend.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadExpiredSuspensionPort {

    /**
     * 해제 대상 회원 ID(중복 없음): suspendedUntil &lt; now 인 정지가 있고, 회원이 현재 SUSPENDED 이며,
     * suspendedUntil &gt;= now 인 진행 중 정지가 없는 회원. 종료 시각과 같은 순간은 아직 정지다.
     */
    List<Long> findReleasableMemberIds(LocalDateTime now);
}
