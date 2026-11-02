package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * 관리자가 쓰는 회원 능력. member(PR02) 공개 Port(조회 MemberAccountQuery·MemberSummaryQuery, 잔액 ChangeGwangsanUseCase,
 * 상태·역할·가입 처리 ManageMemberUseCase), 강제 탈퇴는 PR11 WithdrawMemberUseCase, 지점 존재는 place 공개 query 를 쓴다.
 * 없는 회원/상세의 예외 구분은 기존 관리자 서비스와 같다.
 */
@Component
@RequiredArgsConstructor
public class AdminMemberBridge implements AdminMemberPort {

    private final MemberAccountQuery memberAccountQuery;
    private final MemberSummaryQuery memberSummaryQuery;
    private final ChangeGwangsanUseCase changeGwangsanUseCase;
    private final ManageMemberUseCase manageMemberUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;
    private final PlaceQuery placeQuery;

    @Override
    public AdminPlace loadPlace(Long memberId) {
        return toPlace(withDetail(memberId).orElseThrow(NotFoundMemberDetailException::new));
    }

    @Override
    public AdminPlace loadViewerPlace(Long memberId) {
        // 기존 findPlaceByMemberId: 상세(지점)가 없으면 NotFoundMember.
        return toPlace(withDetail(memberId).orElseThrow(NotFoundMemberException::new));
    }

    @Override
    public Integer loadAdjustTargetPlaceId(Long memberId) {
        // 기존 AdjustGwangsan: 대상 상세가 없으면 NotFoundMember.
        return withDetail(memberId).orElseThrow(NotFoundMemberException::new).placeId();
    }

    @Override
    public void requireMember(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public boolean isPendingMember(Long memberId) {
        return memberAccountQuery.findById(memberId)
                .filter(account -> account.status() == MemberStatus.PENDING)
                .isPresent();
    }

    @Override
    public void adjustGwangsan(Long memberId, Integer gwangsan) {
        changeGwangsanUseCase.adjust(memberId, gwangsan);
    }

    @Override
    public void creditSignUpReward(Long memberId, int gwangsan) {
        changeGwangsanUseCase.credit(memberId, gwangsan, GwangsanLedgerType.SIGNUP_REWARD, memberId);
    }

    @Override
    public void changeRole(Long memberId, ActorContext.Role role, Integer placeId) {
        if (placeId != null) {
            placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new);
        }
        manageMemberUseCase.changeRole(memberId, MemberRole.valueOf(role.name()), placeId);
    }

    @Override
    public void changeStatus(Long memberId, AccountStatus status) {
        manageMemberUseCase.changeStatus(memberId, MemberStatus.valueOf(status.name()));
    }

    @Override
    public void withdrawBanned(Long memberId) {
        withdrawMemberUseCase.execute(new WithdrawMemberCommand(memberId, true));
    }

    @Override
    public SignUpTarget loadSignUpTarget(Long memberId) {
        MemberSummary member = withDetail(memberId).orElseThrow(NotFoundMemberDetailException::new);
        // 기존과 같이 추천인이 없으면 NPE(500)다.
        Long recommenderId = Objects.requireNonNull(member.recommenderId(), "recommender");
        return new SignUpTarget(member.memberId(), member.phoneNumber(), member.placeId(), recommenderId);
    }

    @Override
    public Long rewardRecommender(Long recommenderId, Long newMemberId, int gwangsan) {
        // 기존과 같이 추천인 상세가 없으면 NotFoundMemberDetail. 회원 상세 ID 는 회원 ID 와 같다.
        MemberSummary recommender = withDetail(recommenderId).orElseThrow(NotFoundMemberDetailException::new);
        // 원장 기준 ID 는 신규 회원 ID 다. 같은 가입 승인이 두 번 반영되면 유니크 키가 막는다.
        changeGwangsanUseCase.credit(recommenderId, gwangsan, GwangsanLedgerType.RECOMMENDER_REWARD, newMemberId);
        return recommender.memberId();
    }

    @Override
    public void activate(Long memberId) {
        manageMemberUseCase.changeStatus(memberId, MemberStatus.ACTIVE);
    }

    @Override
    public Optional<Integer> takeWithdrawnGwangsan(String phoneNumber) {
        return manageMemberUseCase.takeWithdrawnGwangsan(phoneNumber);
    }

    @Override
    public void rejectSignUp(Long memberId) {
        manageMemberUseCase.rejectSignUp(memberId);
    }

    private Optional<MemberSummary> withDetail(Long memberId) {
        return memberSummaryQuery.findById(memberId).filter(MemberSummary::hasDetail);
    }

    private static AdminPlace toPlace(MemberSummary member) {
        return new AdminPlace(member.placeId(), member.headId());
    }
}
