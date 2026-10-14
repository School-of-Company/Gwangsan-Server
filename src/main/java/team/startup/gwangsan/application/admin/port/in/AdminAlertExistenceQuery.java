package team.startup.gwangsan.application.admin.port.in;

public interface AdminAlertExistenceQuery {
    boolean exists(Long alertId);
}
