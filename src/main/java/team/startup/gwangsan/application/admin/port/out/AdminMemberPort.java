package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Optional;

/**
 * 관리자가 사용하는 회원 능력. 없는 회원/상세의 예외는 회원 업무의 기존 예외를 그대로 던진다
 * (NotFoundMember / NotFoundMemberDetail 의 구분이 유스케이스마다 다르다).
 */
public interface AdminMemberPort {

    /** 회원 상세의 지점. 상세가 없으면 NotFoundMemberDetail. */
    AdminPlace loadPlace(Long memberId);

    /** 알림 목록 기준 지점. 없으면 NotFoundMember (기존 findPlaceByMemberId). */
    AdminPlace loadViewerPlace(Long memberId);

    /** 광산 조정 대상의 지점. 상세가 없으면 NotFoundMember (기존 AdjustGwangsan 계약). */
    Integer loadAdjustTargetPlaceId(Long memberId);

    /** 회원이 없으면 NotFoundMember. */
    void requireMember(Long memberId);

    boolean isPendingMember(Long memberId);

    void adjustGwangsan(Long memberId, Integer gwangsan);

    /** 가입 보상(재가입 복원액 포함). 원장 기준 ID 는 회원 ID 라 회원당 한 번만 반영된다. */
    void creditSignUpReward(Long memberId, int gwangsan);

    /** placeId 가 있으면 지점(없으면 PlaceNotFound)을 먼저 바꾸고 역할을 바꾼다. */
    void changeRole(Long memberId, ActorContext.Role role, Integer placeId);

    void changeStatus(Long memberId, AccountStatus status);

    /** 관리자 강제 탈퇴. 탈퇴 기록(banned)과 회원 삭제를 현재 트랜잭션에서 수행한다. */
    void withdrawBanned(Long memberId);

    /** 가입 승인 대상. 상세가 없으면 NotFoundMemberDetail. */
    SignUpTarget loadSignUpTarget(Long memberId);

    /** 추천인 보상 후 추천인 회원 상세 ID 를 돌려준다. 상세가 없으면 NotFoundMemberDetail. */
    Long rewardRecommender(Long recommenderId, Long newMemberId, int gwangsan);

    void activate(Long memberId);

    /** 같은 번호의 탈퇴 기록이 있으면 지우고 남긴 광산을 돌려준다. */
    Optional<Integer> takeWithdrawnGwangsan(String phoneNumber);

    /** 가입 기각: 회원 상세를 지운다. */
    void rejectSignUp(Long memberId);
}
