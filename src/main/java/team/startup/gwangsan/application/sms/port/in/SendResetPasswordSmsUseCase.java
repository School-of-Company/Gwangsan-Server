package team.startup.gwangsan.application.sms.port.in;

public interface SendResetPasswordSmsUseCase {

    void execute(String phoneNumber);
}
