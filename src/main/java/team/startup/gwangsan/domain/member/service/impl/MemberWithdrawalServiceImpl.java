package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.domain.member.service.MemberWithdrawalService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalServiceImpl implements MemberWithdrawalService {

    private final MemberUtil memberUtil;
    private final MemberRepository memberRepository;
    private final WithdrawalRecordRepository withdrawalRecordRepository;

    @Override
    @Transactional
    public void execute() {
        Member member = memberUtil.getCurrentMember();

        saveWithdrawalRecord(member.getPhoneNumber());

        memberRepository.delete(member);
    }

    private void saveWithdrawalRecord(String phoneNumber) {
        withdrawalRecordRepository.save(createWithdrawalRecord(phoneNumber));
    }

    private WithdrawalRecord createWithdrawalRecord(String phoneNumber) {
        return WithdrawalRecord.builder()
                .phoneNumber(phoneNumber)
                .gwangsan(0)
                .build();
    }
}
