package team.startup.gwangsan.application.alert.port.out;

import team.startup.gwangsan.domain.alert.model.Alert;

import java.util.List;

public interface SaveAlertPort {
    /** 알림 1건과 수신자별 미확인 receipt 를 순서대로 저장하고 알림 ID 를 돌려준다. */
    Long save(Alert alert, List<Long> recipientMemberIds);
}
