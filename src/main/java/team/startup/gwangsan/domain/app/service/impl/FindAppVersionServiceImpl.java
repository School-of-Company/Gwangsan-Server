package team.startup.gwangsan.domain.app.service.impl;

import java.util.Arrays;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.app.entity.AppVersion;
import team.startup.gwangsan.domain.app.exception.AppVersionUnavailableException;
import team.startup.gwangsan.domain.app.presentation.dto.response.AppVersionResponse;
import team.startup.gwangsan.domain.app.repository.AppVersionRepository;
import team.startup.gwangsan.domain.app.service.FindAppVersionService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAppVersionServiceImpl implements FindAppVersionService {
    private static final Pattern VERSION = Pattern.compile("(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})");
    private final AppVersionRepository repository;

    @Override
    public AppVersionResponse execute(String platform) {
        AppVersion version = repository.findById(platform)
                .orElseThrow(AppVersionUnavailableException::new);
        String latest = version.getLatestVersion();
        String minimum = version.getMinimumVersion();
        if (!VERSION.matcher(latest).matches() || !VERSION.matcher(minimum).matches()
                || Arrays.compare(parts(minimum), parts(latest)) > 0) {
            throw new AppVersionUnavailableException();
        }
        return new AppVersionResponse(latest, minimum);
    }

    private int[] parts(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }
}
