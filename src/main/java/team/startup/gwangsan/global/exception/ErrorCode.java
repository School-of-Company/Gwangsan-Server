package team.startup.gwangsan.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // token
    EXPIRED_TOKEN(401, "토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),

    // member
    NOT_FOUND_MEMBER(404, "해당 회원을 찾을 수 없습니다."),

    // server
    INTERNAL_SERVER_ERROR(500, "예기치 못한 서버 에러가 발생했습니다.");

    private final int status;
    private final String message;
}

