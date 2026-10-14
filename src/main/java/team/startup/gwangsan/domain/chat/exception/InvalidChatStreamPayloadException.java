package team.startup.gwangsan.domain.chat.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class InvalidChatStreamPayloadException extends GlobalException {
    public InvalidChatStreamPayloadException() {
        super(ErrorCode.INVALID_CHAT_STREAM_PAYLOAD);
    }
}
