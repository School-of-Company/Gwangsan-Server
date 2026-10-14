package team.startup.gwangsan.application.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.app.data.AppVersionResult;
import team.startup.gwangsan.application.app.port.in.FindAppVersionQuery;
import team.startup.gwangsan.application.app.port.out.LoadAppVersionPort;
import team.startup.gwangsan.domain.app.exception.AppVersionUnavailableException;
import team.startup.gwangsan.domain.app.model.AppVersion;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAppVersionService implements FindAppVersionQuery {

    private final LoadAppVersionPort loadAppVersionPort;

    @Override
    public AppVersionResult query(String platform) {
        AppVersion version = loadAppVersionPort.findByPlatform(platform)
                .orElseThrow(AppVersionUnavailableException::new);
        return new AppVersionResult(version.latestVersion(), version.minimumVersion());
    }
}
