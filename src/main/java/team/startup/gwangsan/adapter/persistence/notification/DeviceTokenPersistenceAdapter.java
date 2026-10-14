package team.startup.gwangsan.adapter.persistence.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.notification.port.out.LoadPushTokensPort;
import team.startup.gwangsan.application.notification.port.out.SaveDeviceTokenPort;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DeviceTokenPersistenceAdapter implements LoadPushTokensPort, SaveDeviceTokenPort {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public List<String> loadTokens(Collection<Long> memberIds) {
        return memberIds.stream()
                .flatMap(memberId -> deviceTokenRepository.findAllByUserId(memberId).stream())
                .filter(Objects::nonNull)
                .map(DeviceToken::getDeviceToken)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void save(String deviceId, Long memberId, String token, OsType osType) {
        deviceTokenRepository.save(DeviceToken.builder()
                .deviceId(deviceId)
                .userId(memberId)
                .deviceToken(token)
                .osType(osType)
                .build());
    }
}
