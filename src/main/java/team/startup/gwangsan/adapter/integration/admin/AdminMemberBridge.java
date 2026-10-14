package team.startup.gwangsan.adapter.integration.admin;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.Optional;

/**
 * 관리자가 쓰는 회원 능력. 잔액 변경은 member(PR02) 공개 command(ChangeGwangsanUseCase), 강제 탈퇴는 PR11 탈퇴 UseCase,
 * 지점 존재 확인은 place(PR03) 공개 query 를 쓴다.
 * legacy bridge: 역할·상태·지점 변경, 승인 대기 확인, 상세 삭제, 탈퇴 기록 회수는 member 공개 command 가 없어 PR02 저장 모델을 쓴다.
 * 제거: member 관리자용 공개 command → PR12.
 */
@Component
@RequiredArgsConstructor
public class AdminMemberBridge implements AdminMemberPort {

    private final MemberJpaRepository memberRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final WithdrawalRecordJpaRepository withdrawalRecordRepository;
    private final ChangeGwangsanUseCase changeGwangsanUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;
    private final PlaceQuery placeQuery;
    private final EntityManager entityManager;

    @Override
    public AdminPlace loadPlace(Long memberId) {
        return toPlace(detail(memberId).getPlace());
    }

    @Override
    public AdminPlace loadViewerPlace(Long memberId) {
        return toPlace(memberDetailRepository.findPlaceByMemberId(memberId));
    }

    @Override
    public Integer loadAdjustTargetPlaceId(Long memberId) {
        MemberDetailJpaEntity detail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        return toPlace(detail.getPlace()).placeId();
    }

    @Override
    public void requireMember(Long memberId) {
        member(memberId);
    }

    @Override
    public boolean isPendingMember(Long memberId) {
        return memberRepository.findByStatusAndId(MemberStatus.PENDING, memberId).isPresent();
    }

    @Override
    public void adjustGwangsan(Long memberId, Integer gwangsan) {
        changeGwangsanUseCase.adjust(memberId, gwangsan);
    }

    @Override
    public void plusGwangsan(Long memberId, int gwangsan) {
        changeGwangsanUseCase.plus(memberId, gwangsan);
    }

    @Override
    public void changeRole(Long memberId, ActorContext.Role role, Integer placeId) {
        if (placeId != null) {
            Integer existing = placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new).id();
            detail(memberId).updatePlace(entityManager.getReference(PlaceJpaEntity.class, existing));
        }
        member(memberId).updateMemberRole(MemberRole.valueOf(role.name()));
    }

    @Override
    public void changeStatus(Long memberId, AccountStatus status) {
        member(memberId).updateMemberStatus(MemberStatus.valueOf(status.name()));
    }

    @Override
    public void withdrawBanned(Long memberId) {
        withdrawMemberUseCase.execute(new WithdrawMemberCommand(memberId, true));
    }

    @Override
    public SignUpTarget loadSignUpTarget(Long memberId) {
        MemberDetailJpaEntity detail = detail(memberId);
        MemberJpaEntity member = detail.getMember();
        // 기존과 같이 추천인이 없으면 NPE(500)다.
        return new SignUpTarget(member.getId(), member.getPhoneNumber(), toPlace(detail.getPlace()).placeId(),
                member.getRecommender().getId());
    }

    @Override
    public Long rewardRecommender(Long recommenderId, int gwangsan) {
        // 기존과 같이 추천인 상세가 없으면 NotFoundMemberDetail. 회원 상세 ID 는 회원 ID 와 같다.
        MemberDetailJpaEntity recommender = detail(recommenderId);
        changeGwangsanUseCase.plus(recommenderId, gwangsan);
        return recommender.getId();
    }

    @Override
    public void activate(Long memberId) {
        member(memberId).updateMemberStatus(MemberStatus.ACTIVE);
    }

    @Override
    public Optional<Integer> takeWithdrawnGwangsan(String phoneNumber) {
        Optional<WithdrawalRecordJpaEntity> record = withdrawalRecordRepository.findByPhoneNumber(phoneNumber);
        record.ifPresent(withdrawalRecordRepository::delete);
        return record.map(WithdrawalRecordJpaEntity::getGwangsan);
    }

    @Override
    public void rejectSignUp(Long memberId) {
        memberDetailRepository.delete(detail(memberId));
    }

    private MemberJpaEntity member(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    private MemberDetailJpaEntity detail(Long memberId) {
        return memberDetailRepository.findById(memberId).orElseThrow(NotFoundMemberDetailException::new);
    }

    private static AdminPlace toPlace(PlaceJpaEntity place) {
        if (place == null) {
            return new AdminPlace(null, null);
        }
        return new AdminPlace(place.getId(), place.getHead() == null ? null : place.getHead().getId());
    }
}
