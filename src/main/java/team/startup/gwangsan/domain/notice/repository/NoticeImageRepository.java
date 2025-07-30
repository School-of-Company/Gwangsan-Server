package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;

import java.util.List;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {
    void deleteByNoticeIdAndImageId(Long noticeId, Long imageId);

    List<NoticeImage> findAllByNotice(Notice notice);

    List<NoticeImage> findAllByNoticeIdIn(List<Long> noticeIds);

    void deleteAllByNotice(Notice notice);
}
