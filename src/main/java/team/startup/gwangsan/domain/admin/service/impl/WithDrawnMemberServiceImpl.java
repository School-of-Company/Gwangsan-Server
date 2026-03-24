package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.WithDrawnMemberService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class WithDrawnMemberServiceImpl implements WithDrawnMemberService {

    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final AdminAlertRepository adminAlertRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void execute(Long memberId, Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        MemberDetail memberDetail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, memberDetail);

//        AdminAlert alert = adminAlertRepository.findById(alertId)
//                .orElseThrow(NotFoundAdminAlertException::new);
//
//        memberDetail.getMember().updateMemberStatus(MemberStatus.WITHDRAWN);
//
//        applicationEventPublisher.publishEvent(new CreateAlertEvent(
//                alertId,
//                alert.getRequester().getId(),
//                AlertType.REPORT
//        ));

        if (alertId != null) {
            AdminAlert alert = adminAlertRepository.findById(alertId)
                    .orElseThrow(NotFoundAdminAlertException::new);

            memberDetail.getMember().updateMemberStatus(MemberStatus.WITHDRAWN);

            applicationEventPublisher.publishEvent(new CreateAlertEvent(
                    alertId,
                    alert.getRequester().getId(),
                    AlertType.REPORT
            ));
        }
    }
}
