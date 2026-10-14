package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Objects;

/** 실행 관리자의 지점 범위를 만든다. 관리자 상세가 없으면 NotFoundMemberDetail. */
@Component
@RequiredArgsConstructor
public class AdminScopeResolver {

    private final AdminMemberPort adminMemberPort;
    private final AdminPlacePort adminPlacePort;

    public AdminPlaceScope resolve(ActorContext admin) {
        AdminPlace place = adminMemberPort.loadPlace(admin.memberId());
        if (admin.role() == ActorContext.Role.ROLE_HEAD_ADMIN) {
            // 기존 ValidatePlaceUtil 과 같이 지점 없는 본부 관리자는 NPE(500)다.
            Objects.requireNonNull(place.placeId(), "admin place");
            return AdminPlaceScope.ofHead(adminPlacePort.placeIdsInHead(place.headId()));
        }
        return AdminPlaceScope.ofPlace(place.placeId());
    }
}
