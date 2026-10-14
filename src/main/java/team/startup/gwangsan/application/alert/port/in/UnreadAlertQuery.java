package team.startup.gwangsan.application.alert.port.in;

public interface UnreadAlertQuery {
    boolean hasUnread(Long memberId);
}
