package team.startup.gwangsan.adapter.integration.admin;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

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
        private final MemberJpaRepository memberRepository = mock(MemberJpaRepository.class);
        private final MemberDetailJpaRepository memberDetailRepository = mock(MemberDetailJpaRepository.class);
        private final WithdrawalRecordJpaRepository withdrawalRecordRepository = mock(WithdrawalRecordJpaRepository.class);
        private final ChangeGwangsanUseCase changeGwangsanUseCase = mock(ChangeGwangsanUseCase.class);
        private final WithdrawMemberUseCase withdrawMemberUseCase = mock(WithdrawMemberUseCase.class);
        private final PlaceQuery placeQuery = mock(PlaceQuery.class);
        private final AdminMemberBridge bridge = new AdminMemberBridge(memberRepository, memberDetailRepository,
                withdrawalRecordRepository, changeGwangsanUseCase, withdrawMemberUseCase, placeQuery, mock(EntityManager.class));

        @Test
        @DisplayName("it_광산 조정 대상 상세가 없으면 기존처럼 NotFoundMember 다")
        void it_keeps_adjust_not_found_member() {
            when(memberDetailRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge.loadAdjustTargetPlaceId(5L)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("it_일반 지점 조회는 상세가 없으면 NotFoundMemberDetail 이다")
        void it_throws_not_found_member_detail() {
            when(memberDetailRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge.loadPlace(5L)).isInstanceOf(NotFoundMemberDetailException.class);
        }

        @Test
        @DisplayName("it_광산 조정·가입 보상은 member 공개 잔액 command 로 위임한다")
        void it_delegates_gwangsan_changes() {
            bridge.adjustGwangsan(5L, -300);
            bridge.plusGwangsan(5L, 10000);

            verify(changeGwangsanUseCase).adjust(5L, -300);
            verify(changeGwangsanUseCase).plus(5L, 10000);
        }

        @Test
        @DisplayName("it_역할 변경 시 지점이 없으면 PlaceNotFound 이고 역할을 바꾸지 않는다")
        void it_does_not_change_role_when_place_missing() {
            MemberJpaEntity member = MemberJpaEntity.builder().nickname("m").role(MemberRole.ROLE_USER).build();
            when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
            when(placeQuery.findById(3)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bridge.changeRole(5L, ActorContext.Role.ROLE_PLACE_ADMIN, 3))
                    .isInstanceOf(PlaceNotFoundException.class);
            assertThat(member.getRole()).isEqualTo(MemberRole.ROLE_USER);
        }

        @Test
        @DisplayName("it_역할만 바꿀 때는 지점을 조회하지 않는다")
        void it_updates_role_only() {
            MemberJpaEntity member = MemberJpaEntity.builder().nickname("m").role(MemberRole.ROLE_USER).build();
            when(memberRepository.findById(5L)).thenReturn(Optional.of(member));

            bridge.changeRole(5L, ActorContext.Role.ROLE_HEAD_ADMIN, null);

            assertThat(member.getRole()).isEqualTo(MemberRole.ROLE_HEAD_ADMIN);
            verifyNoInteractions(placeQuery);
        }

        @Test
        @DisplayName("it_탈퇴 기록이 있으면 지우고 남긴 광산을 돌려준다")
        void it_takes_withdrawn_gwangsan() {
            WithdrawalRecordJpaEntity record = WithdrawalRecordJpaEntity.builder()
                    .phoneNumber("010").gwangsan(3200).banned(false).build();
            when(withdrawalRecordRepository.findByPhoneNumber("010")).thenReturn(Optional.of(record));

            assertThat(bridge.takeWithdrawnGwangsan("010")).contains(3200);
            verify(withdrawalRecordRepository).delete(record);
        }

        @Test
        @DisplayName("it_추천인 상세를 확인한 뒤 보상하고 상세 ID 를 돌려준다(상세가 없으면 보상하지 않는다)")
        void it_rewards_recommender() {
            MemberDetailJpaEntity detail = MemberDetailJpaEntity.builder().gwangsan(100).build();
            ReflectionTestUtils.setField(detail, "id", 44L);
            when(memberDetailRepository.findById(44L)).thenReturn(Optional.of(detail));
            when(memberDetailRepository.findById(45L)).thenReturn(Optional.empty());

            assertThat(bridge.rewardRecommender(44L, 5000)).isEqualTo(44L);
            verify(changeGwangsanUseCase).plus(44L, 5000);
            assertThatThrownBy(() -> bridge.rewardRecommender(45L, 5000)).isInstanceOf(NotFoundMemberDetailException.class);
            verify(changeGwangsanUseCase, never()).plus(eq(45L), anyInt());
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
        private final TradeCancelJpaRepository tradeCancelRepository = mock(TradeCancelJpaRepository.class);
        private final AdminTradeCancelBridge bridge = new AdminTradeCancelBridge(tradeCancelQuery, productStatusUseCase,
                approveTradeCancelUseCase, tradeCancelRepository);

        @Test
        @DisplayName("it_상품 ID 는 trade 공개 스칼라 조회, 잠금은 post 공개 command(삭제 포함 잠금)다")
        void it_uses_public_ports_for_lock_path() {
            when(tradeCancelQuery.findProductIds(List.of(40L))).thenReturn(Map.of(40L, 300L));
            when(tradeCancelQuery.findProductIds(List.of(41L))).thenReturn(Map.of());

            assertThat(bridge.productIdOf(40L)).isEqualTo(300L);
            assertThatThrownBy(() -> bridge.productIdOf(41L)).isInstanceOf(NotFoundTradeCancelException.class);
            bridge.lockProduct(300L);
            verify(productStatusUseCase).lockIncludingDeleted(300L);
            verifyNoInteractions(tradeCancelRepository);
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
        @DisplayName("it_PENDING 철회만 REJECTED 로 바꾼다")
        void it_rejects_only_pending() {
            MemberJpaEntity requester = MemberJpaEntity.builder().nickname("r").build();
            ReflectionTestUtils.setField(requester, "id", 7L);
            TradeCancelJpaEntity pending = TradeCancelJpaEntity.builder().member(requester).status(TradeCancelStatus.PENDING).build();
            TradeCancelJpaEntity approved = TradeCancelJpaEntity.builder().member(requester).status(TradeCancelStatus.APPROVED).build();
            when(tradeCancelRepository.findById(40L)).thenReturn(Optional.of(pending));
            when(tradeCancelRepository.findById(41L)).thenReturn(Optional.of(approved));

            assertThat(bridge.rejectPending(40L)).isEqualTo(7L);
            assertThat(pending.getStatus()).isEqualTo(TradeCancelStatus.REJECTED);
            assertThatThrownBy(() -> bridge.rejectPending(41L)).isInstanceOf(CannotPendingTradeCancelException.class);
            assertThat(approved.getStatus()).isEqualTo(TradeCancelStatus.APPROVED);
            verifyNoInteractions(approveTradeCancelUseCase);
        }
    }

}
