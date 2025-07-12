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
    INVALID_MEMBER_PRINCIPAL(401, "현재 인증된 사용자의 정보가 유효하지 않습니다."),
    NOT_FOUND_MEMBER(404, "해당 회원을 찾을 수 없습니다."),
    DUPLICATE_PHONE_NUMBER(409, "이미 존재하는 전화번호입니다."),
    DUPLICATE_NICKNAME(409, "이미 존재하는 닉네임입니다."),
    NOT_FOUND_RECOMMENDER(404, "추천인이 존재하지 않습니다."),
    NOT_FOUND_MEMBER_DETAIL(404, "회원 상세 정보를 찾을 수 없습니다."),
    NOT_ALLOWED_USER_ACCESS(403, "사용자 목록 조회 권한이 없습니다."),

    // dong & place
    DONG_NOT_FOUND(404, "동이 존재하지 않습니다."),
    PLACE_NOT_FOUND(404, "장소가 존재하지 않습니다."),
    PLACE_MISMATCH(403, "게시글과 회원의 지역이 일치하지 않습니다."),

    // sms
    INVALID_VERIFICATION_CODE(401, "인증 코드가 유효하지 않습니다."),
    NOT_FOUND_SMS_AUTH(404, "SMS 인증 정보를 찾을 수 없습니다."),
    SMS_AUTH_NOT_COMPLETED(401, "SMS 인증이 완료되지 않았습니다."),
    TOO_MANY_REQUEST_AUTH_CODE(429, "인증번호 요청 횟수를 초과했습니다."),
    AUTH_CODE_GENERATION_FAILURE(500, "인증번호 생성에 실패하였습니다."),
    NOT_MATCH_RANDOM_CODE(400, "인증 번호가 일치하지 않습니다."),
    NOT_FOUND_PENDING_MEMBER(404, "대기 중인 사용자를 찾을 수 없습니다."),

    // product
    NOT_FOUND_PRODUCT(404, "해당 게시글을 찾을 수 없습니다."),
    FORBIDDEN_PRODUCT(403, "해당 게시글에 접근할 권한이 없습니다."),
    CANNOT_COMPLETE_TRADE(400, "거래 완료를 할 수 없습니다."),
    CANNOT_SELECT_SELF(400, "본인을 거래 대상으로 선택할 수 없습니다."),

    // image
    IMAGE_NOT_FOUND(404, "해당 이미지를 찾을 수 없습니다."),
    NOT_FOUND_IMAGE_IDS(404, "존재하지 않는 이미지 ID가 포함되어 있습니다."),
    IMAGE_UPLOAD_FAILED(500, "이미지 업로드에 실패했습니다."),

    // report
    NOT_FOUND_REPORT(404, "신고 내역을 찾을 수 없습니다."),
    SELF_REPORT_NOT_ALLOWED(403, "본인의 게시물은 신고할 수 없습니다."),
    ALREADY_REPORTED(409, "이미 신고한 대상입니다."),

    // admin
    NOT_FOUND_ALERT_TYPE(404, "존재하지 않는 Alert Type 입니다."),
    NOT_FOUND_ADMIN_ALERT(404, "존재하지 않는 어드민 알림입니다."),

    // fcm
    FAILED_TO_INITIALIZE_FIREBASE(500, "Firebase 초기화에 실패했습니다."),

    // server
    INTERNAL_SERVER_ERROR(500, "예기치 못한 서버 에러가 발생했습니다."),

    // auth
    UNAUTHORIZED(401, "닉네임 또는 비밀번호가 잘못되었습니다."),
    FORBIDDEN(403, "탈퇴한 회원이거나 접근이 제한된 계정입니다."),
    NOT_FOUND_USER(401, "존재하지 않는 사용자입니다."),

    // notice
    NOTICE_NOT_FOUND(404, "해당 공지가 존재하지 않습니다."),
    NOTICE_FORBIDDEN(403, "공지에 대한 권한이 없습니다."),

    // related-keyword
    MEMBER_RELATED_KEYWORD_NOT_FOUND(404, "해당 관련 키워드를 찾을 수 없습니다."),
    MEMBER_RELATED_KEYWORD_FORBIDDEN(403, "해당 관련 키워드에 대한 권한이 없습니다.");

    private final int status;
    private final String message;
}