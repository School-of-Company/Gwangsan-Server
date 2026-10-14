package team.startup.gwangsan.application.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.admin.service.AdminServiceFixture.*;

@DisplayName("AdminScopeResolver 단위 테스트")
class AdminScopeResolverTest {
    private final AdminMemberPort members = mock(AdminMemberPort.class);
    private final AdminPlacePort places = mock(AdminPlacePort.class);
    private final AdminScopeResolver resolver = new AdminScopeResolver(members, places);

    @Test
    @DisplayName("it_본부 관리자는 같은 본부 지점 전체를 관리한다")
    void it_uses_head_places_for_head_admin() {
        givenAdmins(members, places);

        assertThatCode(() -> resolver.resolve(HEAD_ADMIN).validate(2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> resolver.resolve(HEAD_ADMIN).validate(OTHER_PLACE)).isInstanceOf(AdminPlaceMismatchException.class);
    }

    @Test
    @DisplayName("it_지점 관리자는 자기 지점만 관리하고 본부 지점을 조회하지 않는다")
    void it_uses_own_place_for_place_admin() {
        givenAdmins(members, places);

        assertThatThrownBy(() -> resolver.resolve(PLACE_ADMIN).validate(2)).isInstanceOf(AdminPlaceMismatchException.class);
        verify(places, never()).placeIdsInHead(any());
    }

    @Test
    @DisplayName("it_지점 없는 본부 관리자는 기존처럼 NPE 다")
    void it_keeps_npe_for_head_admin_without_place() {
        when(members.loadPlace(HEAD_ADMIN.memberId())).thenReturn(new AdminPlace(null, null));

        assertThatThrownBy(() -> resolver.resolve(HEAD_ADMIN)).isInstanceOf(NullPointerException.class);
    }
}
