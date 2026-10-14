package team.startup.gwangsan.application.post.data;

/**
 * 게시글 응답의 작성자. light 는 표시용 단계(원래 값 / 10, 최소 1)다.
 */
public record ProductAuthor(Long memberId, String nickname, String placeName, Integer light) {

    public static ProductAuthor of(AuthorProfile profile) {
        return new ProductAuthor(profile.memberId(), profile.nickname(), profile.placeName(),
                Math.max(1, profile.light() / 10));
    }
}
