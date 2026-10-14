package team.startup.gwangsan.application.notice.port.in;

/** 공지와 이미지의 연결 행만 지운다(이미지 자체 삭제는 image 업무). */
public interface UnlinkNoticeImageUseCase {
    void unlink(Long noticeId, Long imageId);
}
