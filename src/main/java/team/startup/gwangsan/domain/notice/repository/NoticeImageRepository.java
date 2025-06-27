package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {
    void deleteByNoticeIdAndImageId(Long noticeId, Long imageId);
}
