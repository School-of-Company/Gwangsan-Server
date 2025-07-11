package team.startup.gwangsan.domain.post.presentation.dto.response;

public record GetProductMemberResponse(
        Long memberId,
        String nickname,
        String placeName,
        Integer light
) {
}
