package team.startup.gwangsan.application.notice.port.out;

import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 공지와 공지 이미지 연결 저장소. 호출한 application 트랜잭션에 참여한다.
 */
public interface NoticePort {

    Optional<Notice> findById(Long noticeId);

    // 새 공지면 id·createdAt 이 채워진 공지를 돌려준다.
    Notice save(Notice notice);

    // 이미지 연결을 먼저 지우고 공지를 지운다.
    void delete(Long noticeId);

    // placeIds 의 공지를 id 내림차순으로 lastId 미만에서 size 개 조회한다.
    List<Notice> findPage(List<Integer> placeIds, Long lastId, int size);

    void addImages(Long noticeId, List<Long> imageIds);

    void deleteImages(Long noticeId);

    List<NoticeImageResult> findImages(Long noticeId);

    // 공지 id 로 거른다. key 는 공지 id, 값 순서는 DB 반환 순.
    Map<Long, List<NoticeImageLink>> findImageLinks(List<Long> noticeIds);

    void unlinkImage(Long noticeId, Long imageId);
}
