package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.DeviceTokenPort;
import team.startup.gwangsan.application.notification.data.RegisterDeviceTokenCommand;
import team.startup.gwangsan.application.notification.port.in.RegisterDeviceTokenUseCase;
import team.startup.gwangsan.domain.notification.model.OsType;

/**
 * auth 의 기기 토큰 등록 요청을 notification 공개 Port 로 연결한다. 기기 토큰 저장소는 notification 이 소유한다.
 */
@Component
@RequiredArgsConstructor
public class DeviceTokenAdapter implements DeviceTokenPort {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @Override
    public void register(String deviceId, Long memberId, String deviceToken, String osType) {
        registerDeviceTokenUseCase.register(new RegisterDeviceTokenCommand(
                deviceId,
                memberId,
                deviceToken,
                osType == null ? null : OsType.valueOf(osType)
        ));
    }
}
