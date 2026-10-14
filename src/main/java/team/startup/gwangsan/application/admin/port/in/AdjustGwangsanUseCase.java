package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdjustGwangsanCommand;

public interface AdjustGwangsanUseCase {
    void execute(AdjustGwangsanCommand command);
}
