package team.startup.gwangsan.adapter.integration.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberPlaceBridge 단위 테스트")
class MemberPlaceBridgeTest {

    @Mock private PlaceQuery placeQuery;

    private MemberPlaceBridge bridge() {
        return new MemberPlaceBridge(placeQuery);
    }

    @Nested
    @DisplayName("requireExists() 는")
    class Describe_require_exists {

        @Test
        @DisplayName("지점이 존재하면 통과한다")
        void it_passes_when_place_exists() {
            when(placeQuery.findById(4)).thenReturn(Optional.of(new PlaceResult(4, "신창", 1)));

            assertThatCode(() -> bridge().requireExists(4)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("지점이 없으면 PlaceNotFoundException을 던진다")
        void it_throws_place_not_found_exception() {
            when(placeQuery.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge().requireExists(999))
                    .isInstanceOf(PlaceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("belongsToHead() 는")
    class Describe_belongs_to_head {

        @Test
        @DisplayName("지점의 본부가 같으면 true, 다르거나 본부가 없거나 지점이 없으면 false 를 반환한다")
        void it_checks_place_head() {
            when(placeQuery.findById(4)).thenReturn(Optional.of(new PlaceResult(4, "신창", 1)));
            when(placeQuery.findById(5)).thenReturn(Optional.of(new PlaceResult(5, "본부없음", null)));
            when(placeQuery.findById(999)).thenReturn(Optional.empty());

            assertThat(bridge().belongsToHead(4, 1)).isTrue();
            assertThat(bridge().belongsToHead(4, 2)).isFalse();
            assertThat(bridge().belongsToHead(5, 1)).isFalse();
            assertThat(bridge().belongsToHead(999, 1)).isFalse();
        }
    }
}
