package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.domain.member.service.MemberDeletionService;
import team.startup.gwangsan.domain.member.service.MemberWithdrawalService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalServiceImpl implements MemberWithdrawalService {

    private final MemberUtil memberUtil;
    private final WithdrawalRecordRepository withdrawalRecordRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberDeletionService memberDeletionService;

    @Override
    @Transactional
    public void execute() {
        Member member = memberUtil.getCurrentMember();

        MemberDetail memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);

        withdrawalRecordRepository.save(WithdrawalRecord.builder()
                .phoneNumber(member.getPhoneNumber())
                .gwangsan(memberDetail.getGwangsan())
                .banned(false)
                .build());

        memberDeletionService.delete(member);
    }
}