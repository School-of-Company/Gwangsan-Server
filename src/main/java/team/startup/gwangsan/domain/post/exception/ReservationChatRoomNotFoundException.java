package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

// chat 의 NotFoundChatRoomException 과 같은 ErrorCode 를 써서 HTTP 응답을 유지한다.
public class ReservationChatRoomNotFoundException extends GlobalException {
    public ReservationChatRoomNotFoundException() {
        super(ErrorCode.NOT_FOUND_CHAT_ROOM);
    }
}
