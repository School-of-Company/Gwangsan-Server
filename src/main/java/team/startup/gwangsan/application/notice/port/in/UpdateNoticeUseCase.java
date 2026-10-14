package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.UpdateNoticeCommand;

public interface UpdateNoticeUseCase {
    void execute(UpdateNoticeCommand command);
}
