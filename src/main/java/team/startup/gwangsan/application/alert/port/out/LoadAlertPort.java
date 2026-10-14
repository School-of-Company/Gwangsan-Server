package team.startup.gwangsan.application.alert.port.out;

import team.startup.gwangsan.domain.alert.model.Alert;

import java.util.List;

public interface LoadAlertPort {
    /** 수신 알림을 중복 없이 createdAt 내림차순으로 조회한다. */
    List<Alert> loadReceived(Long memberId);

    boolean existsUnread(Long memberId);
}
