package team.startup.gwangsan.application.notification.port.out;

import team.startup.gwangsan.domain.notification.model.OsType;

public interface SaveDeviceTokenPort {
    void save(String deviceId, Long memberId, String token, OsType osType);
}
