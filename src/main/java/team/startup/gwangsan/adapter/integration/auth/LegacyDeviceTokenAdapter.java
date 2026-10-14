package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.DeviceTokenPort;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.OsType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;

// bridge: notification 기기 등록 공개 command 가 생기면(PR04) 교체한다.
@Component
@RequiredArgsConstructor
public class LegacyDeviceTokenAdapter implements DeviceTokenPort {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void register(String deviceId, Long memberId, String deviceToken, String osType) {
        deviceTokenRepository.save(DeviceToken.builder()
                .deviceId(deviceId)
                .userId(memberId)
                .deviceToken(deviceToken)
                .osType(osType == null ? null : OsType.valueOf(osType))
                .build());
    }
}
