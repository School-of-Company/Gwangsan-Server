package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;

public interface CreateFeedbackUseCase {
    void execute(CreateFeedbackCommand command);
}
