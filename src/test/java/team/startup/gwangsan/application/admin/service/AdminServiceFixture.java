package team.startup.gwangsan.application.admin.service;

import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

import static org.mockito.Mockito.lenient;

/** 관리자 서비스 테스트 공통: 지점 관리자(지점 1)·본부 관리자(본부 10 = 지점 1,2). */
final class AdminServiceFixture {

    static final ActorContext PLACE_ADMIN = new ActorContext(90L, ActorContext.Role.ROLE_PLACE_ADMIN);
    static final ActorContext HEAD_ADMIN = new ActorContext(91L, ActorContext.Role.ROLE_HEAD_ADMIN);
    static final int OWN_PLACE = 1;
    static final int OTHER_PLACE = 9;

    private AdminServiceFixture() {
    }

    static void givenAdmins(AdminMemberPort members, AdminPlacePort places) {
        lenient().when(members.loadPlace(PLACE_ADMIN.memberId())).thenReturn(new AdminPlace(OWN_PLACE, 10));
        lenient().when(members.loadPlace(HEAD_ADMIN.memberId())).thenReturn(new AdminPlace(OWN_PLACE, 10));
        lenient().when(places.placeIdsInHead(10)).thenReturn(List.of(1, 2));
    }

    static void givenMemberPlace(AdminMemberPort members, Long memberId, Integer placeId) {
        lenient().when(members.loadPlace(memberId)).thenReturn(new AdminPlace(placeId, placeId == null ? null : 10));
    }
}
