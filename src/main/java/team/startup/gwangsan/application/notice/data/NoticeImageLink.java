package team.startup.gwangsan.application.notice.data;

// linkId 는 공지-이미지 연결 행(notice_image_id) ID 다. 알림 응답이 기존부터 이 값을 쓴다.
public record NoticeImageLink(Long linkId, Long imageId, String imageUrl) {
}
