package team.startup.gwangsan.domain.suspend.service;

public interface SuspendMemberService {
    void execute(Long memberId, int suspendedDays);
}
