package team.startup.gwangsan.application.auth.port.out;

import team.startup.gwangsan.application.auth.data.NewAccount;
import team.startup.gwangsan.domain.auth.model.Account;

import java.util.Optional;

/** auth 가 필요한 회원 계정 조회·생성·비밀번호 변경. */
public interface AccountPort {

    Optional<Account> findById(Long memberId);

    Optional<Account> findByNickname(String nickname);

    Optional<Account> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    boolean isBannedPhoneNumber(String phoneNumber);

    Long register(NewAccount account);

    void changePassword(Long memberId, String passwordHash);
}
