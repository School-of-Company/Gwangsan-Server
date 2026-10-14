package team.startup.gwangsan.application.report.port.in;

import team.startup.gwangsan.application.report.data.CreateReportCommand;

public interface CreateReportUseCase {
    void execute(CreateReportCommand command);
}
