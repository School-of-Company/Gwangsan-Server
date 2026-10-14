package team.startup.gwangsan.application.sms.port.in;

public interface SendSmsUseCase {

    void execute(String phoneNumber);
}
