package team.startup.gwangsan.domain.relatedkeyword.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class MemberRelatedKeywordNotFoundException extends GlobalException {
    public MemberRelatedKeywordNotFoundException() {
        super(ErrorCode.MEMBER_RELATED_KEYWORD_NOT_FOUND);
    }
}
