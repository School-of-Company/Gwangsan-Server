package team.startup.gwangsan.application.feedback.port.out;

public interface RequestFeedbackAnswerAlertPort {
    void requestAnswerAlert(Long feedbackId, Long memberId);
}
