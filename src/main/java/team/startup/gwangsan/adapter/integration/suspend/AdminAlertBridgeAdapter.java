package team.startup.gwangsan.adapter.integration.suspend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.suspend.port.out.LoadAdminAlertPort;
import team.startup.gwangsan.application.admin.port.in.AdminAlertExistenceQuery;

// admin 공개 query(AdminAlertExistenceQuery)로 연결한다(PR11).
@Component
@RequiredArgsConstructor
public class AdminAlertBridgeAdapter implements LoadAdminAlertPort {

    private final AdminAlertExistenceQuery adminAlertExistenceQuery;

    @Override
    public boolean exists(Long adminAlertId) {
        return adminAlertExistenceQuery.exists(adminAlertId);
    }
}
