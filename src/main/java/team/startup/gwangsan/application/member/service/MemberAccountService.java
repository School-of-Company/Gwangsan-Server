package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.domain.member.model.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAccountService implements MemberAccountQuery {

    private final LoadMemberPort loadMemberPort;

    @Override
    public Optional<MemberAccount> findById(Long memberId) {
        return loadMemberPort.findById(memberId).map(MemberAccountService::toAccount);
    }

    @Override
    public Optional<MemberAccount> findByNickname(String nickname) {
        return loadMemberPort.findByNickname(nickname).map(MemberAccountService::toAccount);
    }

    @Override
    public Optional<MemberAccount> findByPhoneNumber(String phoneNumber) {
        return loadMemberPort.findByPhoneNumber(phoneNumber).map(MemberAccountService::toAccount);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return loadMemberPort.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return loadMemberPort.existsByNickname(nickname);
    }

    @Override
    public boolean isBannedPhoneNumber(String phoneNumber) {
        return loadMemberPort.isBannedPhoneNumber(phoneNumber);
    }

    @Override
    public List<Long> findExistingIds(Collection<Long> memberIds) {
        return memberIds.isEmpty() ? List.of() : loadMemberPort.findExistingIds(memberIds);
    }

    private static MemberAccount toAccount(Member member) {
        return new MemberAccount(member.getId(), member.getNickname(), member.getPhoneNumber(),
                member.getPasswordHash(), member.getRole(), member.getStatus());
    }
}
