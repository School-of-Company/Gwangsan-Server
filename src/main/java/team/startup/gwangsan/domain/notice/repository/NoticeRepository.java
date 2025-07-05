package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}