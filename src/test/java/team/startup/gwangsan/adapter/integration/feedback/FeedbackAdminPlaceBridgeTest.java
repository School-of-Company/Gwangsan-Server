package team.startup.gwangsan.adapter.integration.feedback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackAdminPlaceBridge 단위 테스트")
class FeedbackAdminPlaceBridgeTest {

    @Mock private MemberSummaryQuery memberSummaryQuery;
    @Mock private PlaceQuery placeQuery;

    private FeedbackAdminPlaceBridge bridge() {
        return new FeedbackAdminPlaceBridge(memberSummaryQuery, placeQuery);
    }

    @Nested
    @DisplayName("ownPlaceId() 는")
    class Describe_own_place_id {

        @Test
        @DisplayName("상세가 있는 회원의 지점 ID를 돌려준다")
        void it_returns_place_id() {
            when(memberSummaryQuery.findById(2L)).thenReturn(Optional.of(
                    new MemberSummary(2L, "관리자", "01000000000", null, null, 4, 1, "신창", 10)));

            assertThat(bridge().ownPlaceId(2L)).isEqualTo(4);
        }

        @Test
        @DisplayName("상세가 없으면 NotFoundMemberDetailException을 던진다")
        void it_throws_when_no_detail() {
            when(memberSummaryQuery.findById(2L)).thenReturn(Optional.of(
                    new MemberSummary(2L, "관리자", "01000000000", null, null, null, null, null, null)));

            assertThatThrownBy(() -> bridge().ownPlaceId(2L)).isInstanceOf(NotFoundMemberDetailException.class);
        }
    }

    @Nested
    @DisplayName("requirePlace() 는")
    class Describe_require_place {

        @Test
        @DisplayName("지점이 없으면 PlaceNotFoundException을 던진다")
        void it_throws_when_place_missing() {
            when(placeQuery.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge().requirePlace(999)).isInstanceOf(PlaceNotFoundException.class);
        }

        @Test
        @DisplayName("지점이 있으면 그 ID를 돌려준다")
        void it_returns_place_id_when_exists() {
            when(placeQuery.findById(4)).thenReturn(Optional.of(new PlaceResult(4, "신창", 1)));

            assertThat(bridge().requirePlace(4)).isEqualTo(4);
        }
    }
}
