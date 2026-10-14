package team.startup.gwangsan.application.auth.port.in;

public interface FindNicknameQuery {

    /** SMS 인증을 마친 번호의 별칭. 인증 완료 표시는 소비한다. */
    String query(String phoneNumber);
}
