package team.startup.gwangsan.application.suspend.port.in;

/** 다른 업무(alert 등)가 쓰는 정지 공개 조회. */
public interface SuspensionQuery {

    /** @throws team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException suspendId 가 null 이거나 없으면 */
    int getSuspendedDays(Long suspendId);
}
