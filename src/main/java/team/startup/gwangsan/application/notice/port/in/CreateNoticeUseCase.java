package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.CreateNoticeCommand;

public interface CreateNoticeUseCase {
    void execute(CreateNoticeCommand command);
}
