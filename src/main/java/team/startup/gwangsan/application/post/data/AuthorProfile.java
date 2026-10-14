package team.startup.gwangsan.application.post.data;

/**
 * 게시글 작성자 정보. light 는 회원에 저장된 원래 값이다.
 */
public record AuthorProfile(Long memberId, String nickname, String placeName, int light) {
}
