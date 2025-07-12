package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.UpdateMemberRoleService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UpdateMemberRoleServiceImpl implements UpdateMemberRoleService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final AdminAlertRepository adminAlertRepository;

    @Override
    @Transactional
    public void execute(Long memberId, MemberRole role) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        Member targetMember = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        MemberDetail targetMemberDetail = memberDetailRepository.findById(targetMember.getId())
                        .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, targetMemberDetail);

        targetMember.updateMemberRole(role);

        AdminAlert alert = adminAlertRepository.findBySourceIdAndType(memberId, AlertType.SIGN_UP)
                        .orElseThrow(NotFoundAdminAlertException::new);

        adminAlertRepository.delete(alert);
    }
}
