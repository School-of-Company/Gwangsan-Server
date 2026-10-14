package team.startup.gwangsan.domain.app.model;

import team.startup.gwangsan.domain.app.exception.AppVersionUnavailableException;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 플랫폼별 운영 버전 설정. 형식(x.y.z, 앞자리 0 금지)과 minimum ≤ latest 를 만족하지 않는 설정은 만들 수 없다.
 */
public record AppVersion(String latestVersion, String minimumVersion) {

    private static final Pattern VERSION = Pattern.compile("(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})");

    public AppVersion {
        if (latestVersion == null || minimumVersion == null
                || !VERSION.matcher(latestVersion).matches() || !VERSION.matcher(minimumVersion).matches()
                || Arrays.compare(parts(minimumVersion), parts(latestVersion)) > 0) {
            throw new AppVersionUnavailableException();
        }
    }

    private static int[] parts(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }
}
