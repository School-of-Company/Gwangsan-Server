package team.startup.gwangsan.domain.chat.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundChatMessageException extends GlobalException {
    public NotFoundChatMessageException() {
        super(ErrorCode.NOT_FOUND_CHAT_MESSAGE);
    }
}
