package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdminAlertSearch;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;

public interface FindAdminAlertsQuery {
    AdminAlertsResult query(AdminAlertSearch search);
}
