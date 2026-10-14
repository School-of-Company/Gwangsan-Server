package team.startup.gwangsan.adapter.integration.member;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.application.member.data.WithdrawalTarget;
import team.startup.gwangsan.application.member.port.out.MemberWithdrawalPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.util.Optional;

/**
 * 회원 업무 자기 저장(PR02 저장 모델)으로 탈퇴 기록·추천인 재지정·상세/회원 삭제를 수행한다.
 * 제거: PR02 member persistence adapter 로 이 Port 구현을 옮길지 PR12 에서 정리한다.
 */
@Component
@RequiredArgsConstructor
public class MemberWithdrawalBridge implements MemberWithdrawalPort {

    private static final String WITHDRAWN_NICKNAME = "__withdrawn__";

    private final MemberJpaRepository memberRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final WithdrawalRecordJpaRepository withdrawalRecordRepository;
    private final EntityManager entityManager;

    @Override
    public WithdrawalTarget loadTarget(Long memberId) {
        MemberJpaEntity member = memberRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);
        MemberDetailJpaEntity detail = memberDetailRepository.findByMember(member).orElseThrow(NotFoundMemberDetailException::new);
        return new WithdrawalTarget(member.getPhoneNumber(), detail.getGwangsan());
    }

    @Override
    public void saveWithdrawalRecord(String phoneNumber, Integer gwangsan, boolean banned) {
        withdrawalRecordRepository.save(WithdrawalRecordJpaEntity.builder()
                .phoneNumber(phoneNumber)
                .gwangsan(gwangsan)
                .banned(banned)
                .build());
    }

    @Override
    public Optional<Long> findDummyMemberId() {
        return memberRepository.findByNickname(WITHDRAWN_NICKNAME).map(MemberJpaEntity::getId);
    }

    @Override
    public void reassignRecommender(Long memberId, Long dummyMemberId) {
        memberRepository.reassignRecommender(reference(memberId), reference(dummyMemberId));
    }

    @Override
    public void deleteDetail(Long memberId) {
        memberDetailRepository.deleteByMember(reference(memberId));
    }

    @Override
    public void deleteMember(Long memberId) {
        // 앞선 bulk update 의 clearAutomatically 로 영속성 컨텍스트가 비어 있으므로 다시 읽어 지운다.
        memberRepository.deleteById(memberId);
    }

    private MemberJpaEntity reference(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
