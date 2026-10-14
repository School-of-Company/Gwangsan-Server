package team.startup.gwangsan.adapter.persistence.app;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.app.port.out.LoadAppVersionPort;
import team.startup.gwangsan.domain.app.model.AppVersion;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppVersionPersistenceAdapter implements LoadAppVersionPort {

    private final AppVersionJpaRepository repository;

    @Override
    public Optional<AppVersion> findByPlatform(String platform) {
        // 형식이 잘못된 운영 설정은 AppVersion 생성에서 거절된다(기존과 같은 503).
        return repository.findById(platform)
                .map(row -> new AppVersion(row.getLatestVersion(), row.getMinimumVersion()));
    }
}
