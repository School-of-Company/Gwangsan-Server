package team.startup.gwangsan.adapter.integration.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.review.port.out.ReviewMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

/**
 * review → member. 별명은 공개 query 를 쓴다.
 * legacy bridge: 빛(light) 가산은 member 공개 command 가 없어 member 저장소를 직접 쓴다(제거: member 빛 command 추가 시, PR12 대조).
 */
@Component
@RequiredArgsConstructor
public class ReviewMemberBridge implements ReviewMemberPort {

    private final MemberAccountQuery memberAccountQuery;
    private final MemberJpaRepository memberJpaRepository;
    private final MemberDetailJpaRepository memberDetailJpaRepository;

    @Override
    public String findNickname(long memberId) {
        return memberAccountQuery.findById(memberId).map(MemberAccount::nickname).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public void addLight(long memberId, Integer light) {
        memberDetailJpaRepository.findByMember(memberJpaRepository.getReferenceById(memberId))
                .orElseThrow(NotFoundMemberDetailException::new)
                .plusLight(light);
    }
}
