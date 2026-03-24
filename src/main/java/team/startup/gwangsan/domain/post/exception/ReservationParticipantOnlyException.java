package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ReservationParticipantOnlyException extends GlobalException {
    public ReservationParticipantOnlyException() {
        super(ErrorCode.RESERVATION_PARTICIPANT_ONLY);
    }
}
