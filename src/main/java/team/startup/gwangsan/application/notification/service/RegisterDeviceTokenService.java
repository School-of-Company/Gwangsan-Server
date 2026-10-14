package team.startup.gwangsan.application.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.notification.data.RegisterDeviceTokenCommand;
import team.startup.gwangsan.application.notification.port.in.RegisterDeviceTokenUseCase;
import team.startup.gwangsan.application.notification.port.out.SaveDeviceTokenPort;

@Service
@RequiredArgsConstructor
public class RegisterDeviceTokenService implements RegisterDeviceTokenUseCase {

    private final SaveDeviceTokenPort saveDeviceTokenPort;

    @Override
    public void register(RegisterDeviceTokenCommand command) {
        saveDeviceTokenPort.save(command.deviceId(), command.memberId(), command.token(), command.osType());
    }
}
