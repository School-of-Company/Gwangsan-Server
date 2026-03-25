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
    private final MemberRepository memberRepository;
    private final WithdrawalRecordRepository withdrawalRecordRepository;
    private final MemberDetailRepository memberDetailRepository;

    @Override
    @Transactional
    public void execute() {
        Member member = memberUtil.getCurrentMember();

        MemberDetail memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);

        saveWithdrawalRecord(member.getPhoneNumber(), memberDetail.getGwangsan());

        memberRepository.delete(member);
    }

    private void saveWithdrawalRecord(String phoneNumber, int gwangsan) {
        withdrawalRecordRepository.save(createWithdrawalRecord(phoneNumber, gwangsan));
    }

    private WithdrawalRecord createWithdrawalRecord(String phoneNumber, int gwangsan) {
        return WithdrawalRecord.builder()
                .phoneNumber(phoneNumber)
                .gwangsan(gwangsan)
                .build();
    }
}
