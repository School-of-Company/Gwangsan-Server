package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.domain.member.service.MemberWithdrawalService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalServiceImpl implements MemberWithdrawalService {

    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberRepository memberRepository;
    private final WithdrawalRecordRepository withdrawalRecordRepository;

    @Override
    @Transactional
    public void execute() {
        Member member = memberUtil.getCurrentMember();
        MemberDetail memberDetail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        saveWithdrawalRecord(member.getPhoneNumber(), memberDetail.getGwangsan());

        memberDetailRepository.deleteById(memberDetail.getId());
    }

    private void saveWithdrawalRecord(String phoneNumber, Integer gwangsan) {
        withdrawalRecordRepository.save(createWithdrawalRecord(phoneNumber, gwangsan));
    }

    private WithdrawalRecord createWithdrawalRecord(String phoneNumber, Integer gwangsan) {
        return WithdrawalRecord.builder()
                .phoneNumber(phoneNumber)
                .gwangsan(gwangsan)
                .build();
    }
}
