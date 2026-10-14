package team.startup.gwangsan.application.review.data;

/** size 가 null 이면 전체 조회(cursor 도 null). */
public record ReviewPage(Long cursor, Integer size) {

    public static final ReviewPage ALL = new ReviewPage(null, null);
}
