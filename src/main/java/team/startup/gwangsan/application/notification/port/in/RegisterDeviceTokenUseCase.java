package team.startup.gwangsan.application.notification.port.in;

import team.startup.gwangsan.application.notification.data.RegisterDeviceTokenCommand;

public interface RegisterDeviceTokenUseCase {
    void register(RegisterDeviceTokenCommand command);
}
