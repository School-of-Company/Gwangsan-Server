package team.startup.gwangsan.application.post.data;

/** 게시글-이미지 연결 행. linkId 는 연결 행 ID(이미지 ID 와 다름) 로, 알림 응답이 기존부터 이 값을 쓴다. */
public record ProductImageLinkView(Long linkId, Long imageId, String imageUrl) {
}
