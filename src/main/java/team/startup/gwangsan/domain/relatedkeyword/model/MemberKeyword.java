package team.startup.gwangsan.domain.relatedkeyword.model;

import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordForbiddenException;

import java.util.Objects;

/** 회원이 등록한 관심 키워드 매핑. 본인 매핑만 변경할 수 있다. */
public record MemberKeyword(Long id, Long memberId) {

    public void checkOwnedBy(Long actorMemberId) {
        if (!Objects.equals(memberId, actorMemberId)) {
            throw new MemberRelatedKeywordForbiddenException();
        }
    }
}
