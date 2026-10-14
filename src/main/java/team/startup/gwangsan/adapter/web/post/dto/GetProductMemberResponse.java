package team.startup.gwangsan.adapter.web.post.dto;

import team.startup.gwangsan.application.post.data.ProductAuthor;

public record GetProductMemberResponse(
        Long memberId,
        String nickname,
        String placeName,
        Integer light
) {
    static GetProductMemberResponse from(ProductAuthor author) {
        return new GetProductMemberResponse(author.memberId(), author.nickname(), author.placeName(), author.light());
    }
}
