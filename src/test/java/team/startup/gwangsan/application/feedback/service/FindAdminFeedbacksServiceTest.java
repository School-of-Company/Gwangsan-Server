package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.data.AdminFeedbackSearch;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAdminFeedbacksService 단위 테스트")
class FindAdminFeedbacksServiceTest {

    private static final ActorContext HEAD_ADMIN = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);
    private static final ActorContext PLACE_ADMIN = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);

    @Mock private LoadFeedbackPort loadFeedbackPort;
    @Mock private AdminPlaceLookupPort adminPlaceLookupPort;

    private FindAdminFeedbacksService service() {
        return new FindAdminFeedbacksService(loadFeedbackPort, adminPlaceLookupPort,
                new FeedbackAdminAccessGuard(adminPlaceLookupPort));
    }

    @Nested
    @DisplayName("query(AdminFeedbackSearch) 는")
    class Describe_list_query {

        @Test
        @DisplayName("ROLE_HEAD_ADMIN 이 place_id 를 지정하지 않으면 지점 제한 없이 조회한다")
        void it_queries_without_restriction_for_head_admin() {
            AdminFeedbackSearch search = new AdminFeedbackSearch(HEAD_ADMIN, null, null, null, null, 20);

            service().query(search);

            verify(loadFeedbackPort).findAdminDetails(null, null, null, null, null, 20);
            verifyNoHeadScopeLookup();
        }

        @Test
        @DisplayName("ROLE_HEAD_ADMIN 이 place_id 를 지정하면 그 지점으로 엄격히 제한한다")
        void it_restricts_strictly_when_head_admin_specifies_place() {
            when(adminPlaceLookupPort.requirePlace(7)).thenReturn(7);
            AdminFeedbackSearch search = new AdminFeedbackSearch(HEAD_ADMIN, FeedbackStatus.RECEIVED, FeedbackType.ETC, 7, 100L, 20);

            service().query(search);

            verify(loadFeedbackPort).findAdminDetails(FeedbackStatus.RECEIVED, FeedbackType.ETC, 7, null, 100L, 20);
        }

        @Test
        @DisplayName("ROLE_PLACE_ADMIN 은 place_id 파라미터를 무시하고 본인 지점 범위(BUG 예외)로 조회한다")
        void it_uses_own_place_scope_for_place_admin_ignoring_param() {
            when(adminPlaceLookupPort.ownPlaceId(2L)).thenReturn(4);
            AdminFeedbackSearch search = new AdminFeedbackSearch(PLACE_ADMIN, null, null, 999, null, 20);

            service().query(search);

            verify(loadFeedbackPort).findAdminDetails(null, null, null, 4, null, 20);
        }

        private void verifyNoHeadScopeLookup() {
            org.mockito.Mockito.verifyNoInteractions(adminPlaceLookupPort);
        }
    }

    @Nested
    @DisplayName("query(ActorContext, Long) 는")
    class Describe_single_query {

        @Test
        @DisplayName("문의가 없으면 NotFoundFeedbackException 을 던진다")
        void it_throws_when_missing() {
            when(loadFeedbackPort.findAccess(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().query(HEAD_ADMIN, 1L)).isInstanceOf(NotFoundFeedbackException.class);
        }

        @Test
        @DisplayName("PLACE_ADMIN 범위 밖이면 AdminPlaceMismatchException 을 던지고 상세는 조회하지 않는다")
        void it_rejects_out_of_scope_for_place_admin() {
            when(loadFeedbackPort.findAccess(1L)).thenReturn(Optional.of(new FeedbackAccess(FeedbackType.TRADE, 5)));
            when(adminPlaceLookupPort.ownPlaceId(2L)).thenReturn(4);

            assertThatThrownBy(() -> service().query(PLACE_ADMIN, 1L)).isInstanceOf(AdminPlaceMismatchException.class);
            verify(loadFeedbackPort, org.mockito.Mockito.never()).findDetail(any());
        }

        @Test
        @DisplayName("범위 안이면 상세를 반환한다")
        void it_returns_detail_within_scope() {
            when(loadFeedbackPort.findAccess(1L)).thenReturn(Optional.of(new FeedbackAccess(FeedbackType.BUG, 5)));
            FeedbackDetail detail = new FeedbackDetail(1L, 9L, "닉네임", 5, "지점", FeedbackType.BUG, "내용", List.of(),
                    null, null, null, null, FeedbackStatus.RECEIVED, null, null, null, null);
            when(loadFeedbackPort.findDetail(1L)).thenReturn(Optional.of(detail));

            assertThat(service().query(PLACE_ADMIN, 1L)).isEqualTo(detail);
        }
    }
}
