package team.startup.gwangsan.domain.alert.repository.custom;

import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;

import java.util.List;

public interface AlertReceiptCustomRepository {
    List<AlertReceipt> findByMemberIdAndCheckedAndAlertId(Long memberId, boolean check, Long alertId);

    List<Alert> findByMemberId(Long memberId);
}
