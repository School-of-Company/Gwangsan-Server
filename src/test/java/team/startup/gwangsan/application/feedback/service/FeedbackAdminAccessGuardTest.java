package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackAdminAccessGuard 단위 테스트")
class FeedbackAdminAccessGuardTest {

    @Mock private AdminPlaceLookupPort adminPlaceLookupPort;

    private FeedbackAdminAccessGuard guard() {
        return new FeedbackAdminAccessGuard(adminPlaceLookupPort);
    }

    @Nested
    @DisplayName("validate() 는")
    class Describe_validate {

        @Test
        @DisplayName("ROLE_HEAD_ADMIN 은 지점 조회 없이 모든 유형·지점을 통과시킨다")
        void it_allows_head_admin_unconditionally() {
            ActorContext head = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);

            assertThatCode(() -> guard().validate(head, FeedbackType.SUGGESTION, 999)).doesNotThrowAnyException();
            verifyNoInteractions(adminPlaceLookupPort);
        }

        @Test
        @DisplayName("ROLE_PLACE_ADMIN 은 BUG 유형이면 지점 조회 없이 통과시킨다")
        void it_allows_place_admin_for_bug_regardless_of_place() {
            ActorContext placeAdmin = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);

            assertThatCode(() -> guard().validate(placeAdmin, FeedbackType.BUG, 999)).doesNotThrowAnyException();
            verifyNoInteractions(adminPlaceLookupPort);
        }

        @Test
        @DisplayName("ROLE_PLACE_ADMIN 은 BUG 가 아니면 본인 지점과 같을 때만 통과한다")
        void it_allows_place_admin_for_matching_place() {
            ActorContext placeAdmin = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);
            when(adminPlaceLookupPort.ownPlaceId(2L)).thenReturn(4);

            assertThatCode(() -> guard().validate(placeAdmin, FeedbackType.TRADE, 4)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ROLE_PLACE_ADMIN 은 BUG 가 아니고 지점이 다르면 AdminPlaceMismatchException 을 던진다")
        void it_rejects_place_admin_for_other_place() {
            ActorContext placeAdmin = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);
            when(adminPlaceLookupPort.ownPlaceId(2L)).thenReturn(4);

            assertThatThrownBy(() -> guard().validate(placeAdmin, FeedbackType.ACCOUNT, 5))
                    .isInstanceOf(AdminPlaceMismatchException.class);
        }
    }
}
