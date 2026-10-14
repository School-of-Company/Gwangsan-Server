package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.UpdateMemberStatusService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.domain.member.service.MemberDeletionService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UpdateMemberStatusServiceImpl implements UpdateMemberStatusService {

    private final MemberJpaRepository memberRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final WithdrawalRecordJpaRepository withdrawalRecordRepository;
    private final MemberDeletionService memberDeletionService;

    @Override
    @Transactional
    public void execute(Long memberId, MemberStatus status) {
        MemberJpaEntity admin = memberUtil.getCurrentMember();
        MemberDetailJpaEntity adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        MemberJpaEntity targetMember = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        MemberDetailJpaEntity targetMemberDetail = memberDetailRepository.findById(targetMember.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, targetMemberDetail);

        if (status == MemberStatus.WITHDRAWN) {
            withdrawalRecordRepository.save(WithdrawalRecordJpaEntity.builder()
                    .phoneNumber(targetMember.getPhoneNumber())
                    .gwangsan(targetMemberDetail.getGwangsan())
                    .banned(true)
                    .build());
            memberDeletionService.delete(targetMember);
            return;
        }

        targetMember.updateMemberStatus(status);
    }
}