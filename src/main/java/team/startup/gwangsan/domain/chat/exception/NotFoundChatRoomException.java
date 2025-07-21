package team.startup.gwangsan.domain.chat.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundChatRoomException extends GlobalException {
    public NotFoundChatRoomException() {
        super(ErrorCode.NOT_FOUND_CHAT_ROOM);
    }
}
