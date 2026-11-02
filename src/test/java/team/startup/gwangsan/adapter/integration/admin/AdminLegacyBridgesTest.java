package team.startup.gwangsan.adapter.integration.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.RejectTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("관리자 bridge 단위 테스트 (기존 예외 구분·변경 계약 보존)")
class AdminLegacyBridgesTest {

    @Nested
    @DisplayName("AdminMemberBridge 는")
    class Describe_member_bridge {
        private final MemberAccountQuery memberAccountQuery = mock(MemberAccountQuery.class);
        private final MemberSummaryQuery memberSummaryQuery = mock(MemberSummaryQuery.class);
        private final ChangeGwangsanUseCase changeGwangsanUseCase = mock(ChangeGwangsanUseCase.class);
        private final ManageMemberUseCase manageMemberUseCase = mock(ManageMemberUseCase.class);
        private final WithdrawMemberUseCase withdrawMemberUseCase = mock(WithdrawMemberUseCase.class);
        private final PlaceQuery placeQuery = mock(PlaceQuery.class);
        private final AdminMemberBridge bridge = new AdminMemberBridge(memberAccountQuery, memberSummaryQuery,
                changeGwangsanUseCase, manageMemberUseCase, withdrawMemberUseCase, placeQuery);

        private MemberSummary summary(Long id, Long recommenderId, boolean hasDetail) {
            return new MemberSummary(id, "회원" + id, "010" + id, recommenderId, null,
                    hasDetail ? 3 : null, hasDetail ? 10 : null, hasDetail ? "지점" : null, hasDetail ? 40 : null);
        }

        @Test
        @DisplayName("it_광산 조정 대상 상세가 없으면 기존처럼 NotFoundMember, 일반 지점 조회는 NotFoundMemberDetail 이다")
        void it_keeps_not_found_distinction() {
            when(memberSummaryQuery.findById(5L)).thenReturn(Optional.of(summary(5L, null, false)));
            when(memberSummaryQuery.findById(6L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge.loadAdjustTargetPlaceId(5L)).isInstanceOf(NotFoundMemberException.class);
            assertThatThrownBy(() -> bridge.loadPlace(5L)).isInstanceOf(NotFoundMemberDetailException.class);
            assertThatThrownBy(() -> bridge.loadPlace(6L)).isInstanceOf(NotFoundMemberDetailException.class);
            assertThatThrownBy(() -> bridge.loadViewerPlace(5L)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("it_상세가 있으면 지점·본부 ID 를 돌려준다")
        void it_loads_place_and_head() {
            when(memberSummaryQuery.findById(5L)).thenReturn(Optional.of(summary(5L, null, true)));

            assertThat(bridge.loadPlace(5L)).isEqualTo(new AdminPlace(3, 10));
            assertThat(bridge.loadAdjustTargetPlaceId(5L)).isEqualTo(3);
        }

        @Test
        @DisplayName("it_회원 존재·승인 대기 확인은 member 계정 공개 query 를 쓴다")
        void it_uses_account_query() {
            when(memberAccountQuery.findById(5L)).thenReturn(Optional.of(new MemberAccount(5L, "n", "010", "h",
                    MemberRole.ROLE_USER, MemberStatus.PENDING)));
            when(memberAccountQuery.findById(6L)).thenReturn(Optional.empty());

            assertThat(bridge.isPendingMember(5L)).isTrue();
            assertThat(bridge.isPendingMember(6L)).isFalse();
            assertThatThrownBy(() -> bridge.requireMember(6L)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("it_광산 조정·가입 보상은 member 공개 잔액 command 로 위임한다")
        void it_delegates_gwangsan_changes() {
            bridge.adjustGwangsan(5L, -300);
            bridge.creditSignUpReward(5L, 10000);

            verify(changeGwangsanUseCase).adjust(5L, -300);
            verify(changeGwangsanUseCase).credit(5L, 10000, GwangsanLedgerType.SIGNUP_REWARD, 5L);
        }

        @Test
        @DisplayName("it_역할 변경 시 지점이 없으면 PlaceNotFound 이고 역할을 바꾸지 않는다")
        void it_does_not_change_role_when_place_missing() {
            when(placeQuery.findById(3)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge.changeRole(5L, ActorContext.Role.ROLE_PLACE_ADMIN, 3))
                    .isInstanceOf(PlaceNotFoundException.class);
            verifyNoInteractions(manageMemberUseCase);
        }

        @Test
        @DisplayName("it_역할만 바꿀 때는 지점을 조회하지 않고 member 관리 command 로 위임한다")
        void it_updates_role_only() {
            bridge.changeRole(5L, ActorContext.Role.ROLE_HEAD_ADMIN, null);

            verify(manageMemberUseCase).changeRole(5L, MemberRole.ROLE_HEAD_ADMIN, null);
            verifyNoInteractions(placeQuery);
        }

        @Test
        @DisplayName("it_상태 변경·활성화·가입 기각·탈퇴 기록 회수는 member 관리 command 로 위임한다")
        void it_delegates_member_management() {
            when(manageMemberUseCase.takeWithdrawnGwangsan("010")).thenReturn(Optional.of(3200));

            bridge.changeStatus(5L, AccountStatus.SUSPENDED);
            bridge.activate(6L);
            bridge.rejectSignUp(7L);

            assertThat(bridge.takeWithdrawnGwangsan("010")).contains(3200);
            verify(manageMemberUseCase).changeStatus(5L, MemberStatus.SUSPENDED);
            verify(manageMemberUseCase).changeStatus(6L, MemberStatus.ACTIVE);
            verify(manageMemberUseCase).rejectSignUp(7L);
        }

        @Test
        @DisplayName("it_가입 대상은 상세가 있어야 하고 추천인이 없으면 기존처럼 NPE 다")
        void it_loads_sign_up_target() {
            when(memberSummaryQuery.findById(20L)).thenReturn(Optional.of(summary(20L, 4L, true)));
            when(memberSummaryQuery.findById(21L)).thenReturn(Optional.of(summary(21L, null, true)));
            when(memberSummaryQuery.findById(22L)).thenReturn(Optional.of(summary(22L, 4L, false)));

            assertThat(bridge.loadSignUpTarget(20L)).isEqualTo(new SignUpTarget(20L, "01020", 3, 4L));
            assertThatThrownBy(() -> bridge.loadSignUpTarget(21L)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> bridge.loadSignUpTarget(22L)).isInstanceOf(NotFoundMemberDetailException.class);
        }

        @Test
        @DisplayName("it_추천인 상세를 확인한 뒤 보상하고 상세 ID(=회원 ID)를 돌려준다(상세가 없으면 보상하지 않는다)")
        void it_rewards_recommender() {
            when(memberSummaryQuery.findById(44L)).thenReturn(Optional.of(summary(44L, null, true)));
            when(memberSummaryQuery.findById(45L)).thenReturn(Optional.of(summary(45L, null, false)));

            assertThat(bridge.rewardRecommender(44L, 20L, 5000)).isEqualTo(44L);
            verify(changeGwangsanUseCase).credit(44L, 5000, GwangsanLedgerType.RECOMMENDER_REWARD, 20L);
            assertThatThrownBy(() -> bridge.rewardRecommender(45L, 21L, 5000)).isInstanceOf(NotFoundMemberDetailException.class);
            verify(changeGwangsanUseCase, never()).credit(eq(45L), anyInt(), any(), anyLong());
        }

        @Test
        @DisplayName("it_강제 탈퇴는 banned=true 탈퇴 유스케이스를 호출한다")
        void it_withdraws_banned() {
            bridge.withdrawBanned(5L);

            verify(withdrawMemberUseCase).execute(new WithdrawMemberCommand(5L, true));
        }
    }

    @Nested
    @DisplayName("AdminTradeCancelBridge 는")
    class Describe_trade_cancel_bridge {
        private final TradeCancelQuery tradeCancelQuery = mock(TradeCancelQuery.class);
        private final ProductStatusUseCase productStatusUseCase = mock(ProductStatusUseCase.class);
        private final ApproveTradeCancelUseCase approveTradeCancelUseCase = mock(ApproveTradeCancelUseCase.class);
        private final RejectTradeCancelUseCase rejectTradeCancelUseCase = mock(RejectTradeCancelUseCase.class);
        private final AdminTradeCancelBridge bridge = new AdminTradeCancelBridge(tradeCancelQuery, productStatusUseCase,
                approveTradeCancelUseCase, rejectTradeCancelUseCase);

        @Test
        @DisplayName("it_상품 ID 는 trade 공개 스칼라 조회, 잠금은 post 공개 command(삭제 포함 잠금)다")
        void it_uses_public_ports_for_lock_path() {
            when(tradeCancelQuery.findProductIds(List.of(40L))).thenReturn(Map.of(40L, 300L));
            when(tradeCancelQuery.findProductIds(List.of(41L))).thenReturn(Map.of());

            assertThat(bridge.productIdOf(40L)).isEqualTo(300L);
            assertThatThrownBy(() -> bridge.productIdOf(41L)).isInstanceOf(NotFoundTradeCancelException.class);
            bridge.lockProduct(300L);
            verify(productStatusUseCase).lockIncludingDeleted(300L);
            verifyNoInteractions(rejectTradeCancelUseCase);
        }

        @Test
        @DisplayName("it_승인은 trade 공개 command 에 위임하고 참여자 검사를 그대로 전달한다")
        void it_delegates_approval_with_check() {
            AdminTradeCancelPort.ParticipantCheck check = mock(AdminTradeCancelPort.ParticipantCheck.class);

            bridge.approve(40L, check);

            ArgumentCaptor<ApproveTradeCancelCommand> captor = ArgumentCaptor.forClass(ApproveTradeCancelCommand.class);
            verify(approveTradeCancelUseCase).execute(captor.capture());
            assertThat(captor.getValue().tradeCancelId()).isEqualTo(40L);
            captor.getValue().authorizer().authorize(7L, 8L);
            verify(check).check(7L, 8L);
        }

        @Test
        @DisplayName("it_기각과 요청자 조회는 trade 공개 Port 로 위임하고, 요청자가 없으면 NotFoundTradeCancel 이다")
        void it_delegates_reject_and_requester() {
            when(rejectTradeCancelUseCase.reject(40L)).thenReturn(7L);
            when(rejectTradeCancelUseCase.reject(41L)).thenThrow(new CannotPendingTradeCancelException());
            when(tradeCancelQuery.findRequesterId(40L)).thenReturn(Optional.of(7L));
            when(tradeCancelQuery.findRequesterId(42L)).thenReturn(Optional.empty());

            assertThat(bridge.rejectPending(40L)).isEqualTo(7L);
            assertThatThrownBy(() -> bridge.rejectPending(41L)).isInstanceOf(CannotPendingTradeCancelException.class);
            assertThat(bridge.requesterIdOf(40L)).isEqualTo(7L);
            assertThatThrownBy(() -> bridge.requesterIdOf(42L)).isInstanceOf(NotFoundTradeCancelException.class);
            verifyNoInteractions(approveTradeCancelUseCase);
        }
    }
}
