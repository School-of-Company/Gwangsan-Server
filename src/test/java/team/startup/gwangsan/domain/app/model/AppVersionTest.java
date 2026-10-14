package team.startup.gwangsan.domain.app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import team.startup.gwangsan.domain.app.exception.AppVersionUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppVersion 운영 설정 규칙")
class AppVersionTest {

    @Nested
    @DisplayName("생성은")
    class Describe_create {

        @ParameterizedTest
        @CsvSource({"1.10.0,1.9.0", "1.10.0,1.10.0", "0.0.0,0.0.0", "999999999.0.0,1.0.0"})
        @DisplayName("x.y.z 형식이고 minimum ≤ latest(숫자 비교)면 값을 그대로 유지한다")
        void it_accepts_valid_versions(String latest, String minimum) {
            AppVersion version = new AppVersion(latest, minimum);

            assertThat(version.latestVersion()).isEqualTo(latest);
            assertThat(version.minimumVersion()).isEqualTo(minimum);
        }

        @ParameterizedTest
        @CsvSource({"1.0,1.0.0", "01.10.0,1.0.0", "1.0.0-beta,1.0.0", "1000000000.0.0,1.0.0", "1.8.0,1.9.0", "1.10.0,bad", "' 1.0.0',1.0.0"})
        @DisplayName("형식 오류나 minimum > latest 면 AppVersionUnavailableException 을 던진다")
        void it_rejects_malformed_or_inverted_versions(String latest, String minimum) {
            assertThatThrownBy(() -> new AppVersion(latest, minimum))
                    .isInstanceOf(AppVersionUnavailableException.class);
        }

        @Test
        @DisplayName("값이 없으면 AppVersionUnavailableException 을 던진다")
        void it_rejects_null() {
            assertThatThrownBy(() -> new AppVersion(null, "1.0.0")).isInstanceOf(AppVersionUnavailableException.class);
            assertThatThrownBy(() -> new AppVersion("1.0.0", null)).isInstanceOf(AppVersionUnavailableException.class);
        }
    }
}
