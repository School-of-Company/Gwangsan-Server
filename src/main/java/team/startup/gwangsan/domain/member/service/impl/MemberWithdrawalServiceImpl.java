package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.domain.member.service.MemberDeletionService;
import team.startup.gwangsan.domain.member.service.MemberWithdrawalService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalServiceImpl implements MemberWithdrawalService {

    private final MemberUtil memberUtil;
    private final WithdrawalRecordJpaRepository withdrawalRecordRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final MemberDeletionService memberDeletionService;

    @Override
    @Transactional
    public void execute() {
        MemberJpaEntity member = memberUtil.getCurrentMember();

        MemberDetailJpaEntity memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);

        withdrawalRecordRepository.save(WithdrawalRecordJpaEntity.builder()
                .phoneNumber(member.getPhoneNumber())
                .gwangsan(memberDetail.getGwangsan())
                .banned(false)
                .build());

        memberDeletionService.delete(member);
    }
}