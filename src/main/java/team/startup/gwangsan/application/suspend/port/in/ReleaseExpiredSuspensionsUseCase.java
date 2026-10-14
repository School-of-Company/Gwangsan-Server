package team.startup.gwangsan.application.suspend.port.in;

import java.time.LocalDateTime;

public interface ReleaseExpiredSuspensionsUseCase {

    /** @return ACTIVE 로 바꾼 회원 수 */
    int execute(LocalDateTime now);
}
