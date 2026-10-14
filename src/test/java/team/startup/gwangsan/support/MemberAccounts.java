package team.startup.gwangsan.support;

import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberPersistenceAdapter;
import team.startup.gwangsan.adapter.security.auth.MemberDetailsService;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.service.MemberAccountService;

/** 보안 adapter 테스트용: 회원 저장소 mock 위에 실제 member 공개 조회를 연결한다. */
public final class MemberAccounts {

    private MemberAccounts() {
    }

    public static MemberDetailsService detailsService(MemberJpaRepository memberRepository) {
        return new MemberDetailsService(new MemberAccountService(
                new MemberPersistenceAdapter(memberRepository, null, null, null)));
    }

    public static MemberAccount of(MemberJpaEntity member) {
        return new MemberAccount(member.getId(), member.getNickname(), member.getPhoneNumber(), member.getPassword(),
                member.getRole(), member.getStatus());
    }
}
