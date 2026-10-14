package team.startup.gwangsan.application.relatedkeyword.port.in;

import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;

import java.util.List;

/** 회원의 관심 키워드. relatedKeywordId 는 매핑 ID 가 아니라 키워드 ID 다(기존 계약). */
public interface FindMemberRelatedKeywordsQuery {
    List<RelatedKeywordResult> query(Long memberId);
}
