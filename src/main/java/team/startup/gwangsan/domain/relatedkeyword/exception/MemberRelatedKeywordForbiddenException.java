package team.startup.gwangsan.domain.relatedkeyword.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class MemberRelatedKeywordForbiddenException extends GlobalException {
    public MemberRelatedKeywordForbiddenException() {
        super(ErrorCode.MEMBER_RELATED_KEYWORD_FORBIDDEN);
    }
}