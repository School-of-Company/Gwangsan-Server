package team.startup.gwangsan.application.member.port.in;

public interface ChangePasswordUseCase {

    void execute(Long memberId, String passwordHash);
}
