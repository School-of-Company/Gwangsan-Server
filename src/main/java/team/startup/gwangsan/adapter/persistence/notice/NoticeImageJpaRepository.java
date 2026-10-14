package team.startup.gwangsan.adapter.persistence.notice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeImageJpaRepository extends JpaRepository<NoticeImageJpaEntity, Long>, NoticeImageJpaCustomRepository {
    void deleteByNoticeIdAndImageId(Long noticeId, Long imageId);

    void deleteAllByNoticeId(Long noticeId);
}
