package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminAlertSearch;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.port.in.FindAdminAlertsQuery;
import team.startup.gwangsan.application.admin.port.out.AdminAlertReadPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAdminAlertsService implements FindAdminAlertsQuery {

    private final AdminMemberPort adminMemberPort;
    private final AdminPlacePort adminPlacePort;
    private final AdminAlertReadPort adminAlertReadPort;

    // 기준선: readOnly 가 아닌 일반 트랜잭션.
    @Override
    @Transactional
    public AdminAlertsResult query(AdminAlertSearch search) {
        return adminAlertReadPort.findAlerts(targetPlaceIds(search), search.type());
    }

    // HEAD_ADMIN 은 place_id 로 임의 지점을 고를 수 있고(기존 동작), 없으면 본부 전체를 본다.
    // PLACE_ADMIN 은 place_id 를 무시하고 자기 지점만 본다.
    private List<Integer> targetPlaceIds(AdminAlertSearch search) {
        ActorContext admin = search.actor();
        if (admin.role() == ActorContext.Role.ROLE_HEAD_ADMIN) {
            if (search.placeId() == null) {
                AdminPlace place = adminMemberPort.loadViewerPlace(admin.memberId());
                return adminPlacePort.placeIdsInHead(place.headId());
            }
            return List.of(adminPlacePort.requirePlace(search.placeId()));
        }
        return List.of(adminMemberPort.loadViewerPlace(admin.memberId()).placeId());
    }
}
