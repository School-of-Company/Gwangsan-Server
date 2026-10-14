package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.AdjustGwangsanService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class AdjustGwangsanServiceImpl implements AdjustGwangsanService {

    private final MemberDetailJpaRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    @Transactional
    public void execute(Long memberId, Integer gwangsan) {
        MemberJpaEntity admin = memberUtil.getCurrentMember();
        MemberDetailJpaEntity adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        MemberDetailJpaEntity targetMemberDetail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, targetMemberDetail);
        targetMemberDetail.adjustGwangsan(gwangsan);
    }
}
