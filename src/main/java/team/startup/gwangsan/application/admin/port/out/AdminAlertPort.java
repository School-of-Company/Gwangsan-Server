package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.model.AdminAlert;

import java.util.Optional;

public interface AdminAlertPort {

    Optional<AdminAlert> findById(Long alertId);

    /** 요청자와 inner join 으로 함께 읽는다(요청자 없는 알림은 조회되지 않는다). */
    Optional<AdminAlert> findWithRequester(Long alertId);

    Optional<AdminAlert> findByIdAndType(Long alertId, AlertType type);

    Optional<AdminAlert> findByTypeAndSourceId(AlertType type, Long sourceId);

    boolean existsById(Long alertId);

    void save(AlertType type, String title, Long sourceId, Long requesterId);

    void delete(Long alertId);

    void reassignMember(Long memberId, Long dummyMemberId);
}
