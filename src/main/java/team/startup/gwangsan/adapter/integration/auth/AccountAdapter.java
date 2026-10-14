package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.data.NewAccount;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.data.RegisterMemberCommand;
import team.startup.gwangsan.application.member.port.in.ChangePasswordUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.RegisterMemberUseCase;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.AccountStatus;

import java.util.Optional;

/** auth → member 공개 Port 연결. */
@Component
@RequiredArgsConstructor
public class AccountAdapter implements AccountPort {

    private final MemberAccountQuery memberAccountQuery;
    private final RegisterMemberUseCase registerMemberUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @Override
    public Optional<Account> findById(Long memberId) {
        return memberAccountQuery.findById(memberId).map(AccountAdapter::toAccount);
    }

    @Override
    public Optional<Account> findByNickname(String nickname) {
        return memberAccountQuery.findByNickname(nickname).map(AccountAdapter::toAccount);
    }

    @Override
    public Optional<Account> findByPhoneNumber(String phoneNumber) {
        return memberAccountQuery.findByPhoneNumber(phoneNumber).map(AccountAdapter::toAccount);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return memberAccountQuery.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberAccountQuery.existsByNickname(nickname);
    }

    @Override
    public boolean isBannedPhoneNumber(String phoneNumber) {
        return memberAccountQuery.isBannedPhoneNumber(phoneNumber);
    }

    @Override
    public Long register(NewAccount account) {
        return registerMemberUseCase.execute(new RegisterMemberCommand(account.name(), account.nickname(),
                account.passwordHash(), account.phoneNumber(), account.recommenderId(), account.dongId(),
                account.placeId(), account.description(), account.specialties()));
    }

    @Override
    public void changePassword(Long memberId, String passwordHash) {
        changePasswordUseCase.execute(memberId, passwordHash);
    }

    private static Account toAccount(MemberAccount member) {
        return new Account(member.id(), member.nickname(), member.phoneNumber(), member.passwordHash(),
                member.role().name(), AccountStatus.valueOf(member.status().name()));
    }
}
