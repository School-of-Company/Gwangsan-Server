package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.AnswerFeedbackCommand;

public interface AnswerFeedbackUseCase {
    void execute(AnswerFeedbackCommand command);
}
