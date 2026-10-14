package team.startup.gwangsan.adapter.integration.notification;

import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;

import java.util.List;
import java.util.Objects;

final class LegacyDeviceTokens {

    private LegacyDeviceTokens() {
    }

    static List<Long> memberIds(List<DeviceToken> deviceTokens) {
        if (deviceTokens == null) return List.of();
        return deviceTokens.stream()
                .filter(Objects::nonNull)
                .map(DeviceToken::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
