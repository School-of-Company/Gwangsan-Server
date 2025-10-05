package team.startup.gwangsan.domain.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.repository.custom.AlertReceiptCustomRepository;

public interface AlertReceiptRepository extends JpaRepository<AlertReceipt, Long>, AlertReceiptCustomRepository {
    boolean existsByMemberIdAndChecked(Long memberId, boolean check);
}
