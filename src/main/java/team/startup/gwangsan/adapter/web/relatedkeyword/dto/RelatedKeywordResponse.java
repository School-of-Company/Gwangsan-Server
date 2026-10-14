package team.startup.gwangsan.adapter.web.relatedkeyword.dto;

import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;

public record RelatedKeywordResponse(
        Long relatedKeywordId,
        String name
) {
    public static RelatedKeywordResponse from(RelatedKeywordResult result) {
        return new RelatedKeywordResponse(result.relatedKeywordId(), result.name());
    }
}
