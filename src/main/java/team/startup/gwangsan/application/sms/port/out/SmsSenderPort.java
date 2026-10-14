package team.startup.gwangsan.application.sms.port.out;

public interface SmsSenderPort {

    /** 비동기 발송. 실패는 로그만 남긴다. */
    void sendAsync(String to, String text);

    /** 동기 발송. 실패하면 SmsSendFailedException. */
    void send(String to, String text);
}
