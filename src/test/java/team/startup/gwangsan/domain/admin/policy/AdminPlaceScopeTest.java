package team.startup.gwangsan.domain.admin.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdminPlaceScope 단위 테스트")
class AdminPlaceScopeTest {

    @Nested
    @DisplayName("본부 범위는")
    class Describe_head_scope {
        private final AdminPlaceScope scope = AdminPlaceScope.ofHead(List.of(1, 2, 3));

        @Test
        @DisplayName("같은 본부 지점이면 통과한다")
        void it_passes_when_same_head() {
            assertThatCode(() -> scope.validate(2)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다른 본부 지점이면 기존 PLACE_MISMATCH 오류를 던진다")
        void it_throws_when_different_head() {
            assertThatThrownBy(() -> scope.validate(9))
                    .isInstanceOf(AdminPlaceMismatchException.class)
                    .satisfies(e -> assertThat(((AdminPlaceMismatchException) e).getErrorCode()).isEqualTo(ErrorCode.PLACE_MISMATCH));
        }
    }

    @Nested
    @DisplayName("지점 범위는")
    class Describe_place_scope {
        @ParameterizedTest
        @CsvSource({"5,5,true", "5,6,false"})
        @DisplayName("자기 지점만 통과한다")
        void it_passes_only_own_place(int own, int target, boolean allowed) {
            AdminPlaceScope scope = AdminPlaceScope.ofPlace(own);
            if (allowed) {
                assertThatCode(() -> scope.validate(target)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> scope.validate(target)).isInstanceOf(AdminPlaceMismatchException.class);
            }
        }

        @Test
        @DisplayName("대상 지점이 없으면 기존처럼 NPE 다")
        void it_keeps_null_target_as_npe() {
            assertThatThrownBy(() -> AdminPlaceScope.ofPlace(5).validate(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
