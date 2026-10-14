package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminAlertCommand;
import team.startup.gwangsan.application.admin.data.MemberAlertKind;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.application.admin.port.in.VerifySignUpUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminNotificationPort;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;
import team.startup.gwangsan.domain.admin.policy.SignUpReward;

@Service
@RequiredArgsConstructor
public class VerifySignUpService implements VerifySignUpUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminAlertPort adminAlertPort;
    private final AdminMemberPort adminMemberPort;
    private final AdminNotificationPort adminNotificationPort;

    // 기존 순서 유지: 추천인 보상 뒤에 지점을 검사한다(실패 시 트랜잭션 전체 rollback).
    @Override
    @Transactional
    public void execute(AdminAlertCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        AdminAlert alert = adminAlertPort.findByIdAndType(command.alertId(), AlertType.SIGN_UP)
                .orElseThrow(NotFoundAdminAlertException::new);
        SignUpTarget target = adminMemberPort.loadSignUpTarget(alert.sourceId());
        Long recommenderDetailId = adminMemberPort.rewardRecommender(
                target.recommenderId(), SignUpReward.RECOMMENDER_GWANGSAN_REWARD);
        scope.validate(target.placeId());
        adminMemberPort.activate(target.memberId());
        adminMemberPort.plusGwangsan(target.memberId(),
                SignUpReward.gwangsanFor(adminMemberPort.takeWithdrawnGwangsan(target.phoneNumber())));
        adminAlertPort.delete(alert.id());
        adminNotificationPort.notifyMember(target.memberId(), recommenderDetailId, MemberAlertKind.RECOMMENDER);
    }
}
