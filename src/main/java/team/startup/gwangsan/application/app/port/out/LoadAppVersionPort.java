package team.startup.gwangsan.application.app.port.out;

import team.startup.gwangsan.domain.app.model.AppVersion;

import java.util.Optional;

public interface LoadAppVersionPort {
    Optional<AppVersion> findByPlatform(String platform);
}
