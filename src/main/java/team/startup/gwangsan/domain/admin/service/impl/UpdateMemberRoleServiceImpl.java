package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.UpdateMemberRoleService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UpdateMemberRoleServiceImpl implements UpdateMemberRoleService {

    private final MemberRepository memberRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    @Transactional
    public void execute(Long memberId, MemberRole role) {
        Member admin = memberUtil.getCurrentMember();

        Member targetMember = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validatePlaceUtil.validateSamePlace(admin, targetMember);

        targetMember.updateMemberRole(role);
    }
}
