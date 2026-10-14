package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.DeleteNoticeCommand;

public interface DeleteNoticeUseCase {
    void execute(DeleteNoticeCommand command);
}
