package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.domain.admin.policy.AdminAlertTitle;

/** 없으면 report 업무의 NotFoundReport. */
public interface AdminReportPort {

    AdminAlertTitle.ReportCategory categoryOf(Long reportId);

    Long reporterIdOf(Long reportId);
}
