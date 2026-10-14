package team.startup.gwangsan.application.auth.port.in;

/** 저장된 비밀번호 해시와 입력 비밀번호의 일치 여부. */
public interface PasswordMatchQuery {

    boolean matches(String rawPassword, String passwordHash);
}
