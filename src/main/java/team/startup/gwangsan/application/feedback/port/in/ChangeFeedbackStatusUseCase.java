package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.ChangeFeedbackStatusCommand;

public interface ChangeFeedbackStatusUseCase {
    void execute(ChangeFeedbackStatusCommand command);
}
