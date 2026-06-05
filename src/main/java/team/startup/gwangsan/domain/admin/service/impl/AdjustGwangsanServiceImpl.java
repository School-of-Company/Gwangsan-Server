package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.AdjustGwangsanService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class AdjustGwangsanServiceImpl implements AdjustGwangsanService {

    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    @Transactional
    public void execute(Long memberId, Integer gwangsan) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        MemberDetail targetMemberDetail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, targetMemberDetail);
        targetMemberDetail.adjustGwangsan(gwangsan);
    }
}
