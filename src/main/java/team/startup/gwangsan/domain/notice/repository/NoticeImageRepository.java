package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.repository.custom.NoticeImageCustomRepository;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long>, NoticeImageCustomRepository {
    void deleteByNoticeIdAndImageId(Long noticeId, Long imageId);

    void deleteAllByNotice(Notice notice);
}
