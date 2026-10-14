package team.startup.gwangsan.domain.chat.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

/** stream 저장 시 발신자 부재. 기존 NotFoundMemberException 과 같은 오류 코드, 재시도 없이 DLQ. */
public class NotFoundChatSenderException extends GlobalException {
    public NotFoundChatSenderException() {
        super(ErrorCode.NOT_FOUND_MEMBER);
    }
}
