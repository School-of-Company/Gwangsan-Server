package team.startup.gwangsan.application.relatedkeyword.port.out;

import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;

import java.util.List;
import java.util.Optional;

public interface RelatedKeywordPort {
    List<RelatedKeywordResult> findAll();

    List<RelatedKeywordResult> findAllByMemberId(Long memberId);

    Optional<MemberKeyword> findMemberKeyword(Long memberRelatedKeywordId);

    void deleteMemberKeyword(Long memberRelatedKeywordId);

    /** 즉시 실행되는 bulk 삭제. 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically). */
    void bulkDeleteAllByMemberId(Long memberId);
}
