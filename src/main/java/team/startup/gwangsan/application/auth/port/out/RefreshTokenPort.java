package team.startup.gwangsan.application.auth.port.out;

import java.util.Optional;

/** 회원 번호당 refresh token 하나. 저장 시 덮어쓴다. */
public interface RefreshTokenPort {

    void save(String phoneNumber, String refreshToken);

    Optional<String> findPhoneNumberByToken(String refreshToken);

    void deleteByPhoneNumber(String phoneNumber);
}
