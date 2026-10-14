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

    /** 이름마다 키워드를 찾거나 만들고 회원과 연결한다. */
    void addMemberKeywords(Long memberId, List<String> names);

    /** 파생 삭제로 기존 연결을 지우고 다시 연결한다. 같은 컨텍스트의 dirty 변경을 보존한다. */
    void replaceMemberKeywords(Long memberId, List<String> names);

    /** 즉시 실행되는 bulk 삭제. 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically). */
    void bulkDeleteAllByMemberId(Long memberId);
}
