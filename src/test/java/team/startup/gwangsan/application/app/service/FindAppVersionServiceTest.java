package team.startup.gwangsan.application.app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.app.data.AppVersionResult;
import team.startup.gwangsan.application.app.port.out.LoadAppVersionPort;
import team.startup.gwangsan.domain.app.exception.AppVersionUnavailableException;
import team.startup.gwangsan.domain.app.model.AppVersion;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FindAppVersionService 단위 테스트")
class FindAppVersionServiceTest {

    private final LoadAppVersionPort port = mock(LoadAppVersionPort.class);
    private final FindAppVersionService service = new FindAppVersionService(port);

    @Nested
    @DisplayName("query() 메서드는")
    class Describe_query {

        @Test
        @DisplayName("플랫폼 설정이 있으면 latest/minimum 을 반환한다")
        void it_returns_versions_of_platform() {
            when(port.findByPlatform("ios")).thenReturn(Optional.of(new AppVersion("1.10.0", "1.9.0")));

            assertThat(service.query("ios")).isEqualTo(new AppVersionResult("1.10.0", "1.9.0"));
        }

        @Test
        @DisplayName("플랫폼 설정이 없으면 버전을 지어내지 않고 AppVersionUnavailableException 을 던진다")
        void it_throws_app_version_unavailable_exception() {
            when(port.findByPlatform("android")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.query("android")).isInstanceOf(AppVersionUnavailableException.class);
        }
    }
}
