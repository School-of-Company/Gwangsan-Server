package team.startup.gwangsan.domain.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.repository.custom.AlertCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long>, AlertCustomRepository {
    List<Alert> findAllByMember(Member member);

    boolean existsByMemberAndChecked(Member member, boolean checked);
}
