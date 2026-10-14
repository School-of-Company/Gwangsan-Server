package team.startup.gwangsan.application.notification.data;

import team.startup.gwangsan.domain.notification.model.OsType;

public record RegisterDeviceTokenCommand(
        String deviceId,
        Long memberId,
        String token,
        OsType osType
) {
}
