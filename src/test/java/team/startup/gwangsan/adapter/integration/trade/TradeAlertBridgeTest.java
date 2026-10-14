package team.startup.gwangsan.adapter.integration.trade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.application.alert.port.in.RecordAlertUseCase;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 확정 범위: OTHER_MEMBER_TRADE_COMPLETE·TRADE_CANCEL 만 alert 공개 command(RecordAlertUseCase)로
 * 호출자 트랜잭션에서 저장한다. 같은 알림을 비동기 CreateAlertEvent 로 발행하면 중복 저장되므로 발행하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeAlertBridge 중요 알림 저장 범위")
class TradeAlertBridgeTest {

    @InjectMocks private TradeAlertBridge bridge;

    @Mock private RecordAlertUseCase recordAlertUseCase;
    @Mock private ProductSnapshotQuery productSnapshotQuery;
    @Mock private MemberAccountQuery memberAccountQuery;
    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void it_records_completion_request_alert_with_legacy_wording_without_async_event() {
        when(productSnapshotQuery.get(100L)).thenReturn(new ProductSnapshot(100L, 1L, "상품", "설명", 700,
                Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING));
        when(memberAccountQuery.findById(2L)).thenReturn(Optional.of(
                new MemberAccount(2L, "구매자", "010", "hash", MemberRole.ROLE_USER, MemberStatus.ACTIVE)));

        bridge.recordCompletionRequested(100L, 2L, 1L, 1L);

        ArgumentCaptor<RecordAlertCommand> command = ArgumentCaptor.forClass(RecordAlertCommand.class);
        verify(recordAlertUseCase).record(command.capture());
        assertThat(command.getValue().alert()).isEqualTo(Alert.otherMemberTradeComplete(100L, 1L, "상품", "구매자"));
        assertThat(command.getValue().recipientMemberIds()).containsExactly(1L);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void it_records_cancel_approved_alert_for_both_parties_without_async_event() {
        bridge.recordCancelApproved(20L, 2L, 2L, 1L);

        verify(recordAlertUseCase).record(new RecordAlertCommand(Alert.tradeCancel(20L, 2L), List.of(2L, 1L)));
        verifyNoInteractions(applicationEventPublisher);
    }
}
