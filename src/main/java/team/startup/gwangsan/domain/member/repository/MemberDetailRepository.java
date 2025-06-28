package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.MemberDetail;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long> {
}
