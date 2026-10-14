package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertEvent;
import team.startup.gwangsan.application.admin.port.in.RemoveAdminAlertUseCase;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.application.alert.port.in.RecordAlertUseCase;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.trade.port.out.TradeAlertPort;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.util.List;

/**
 * 거래 알림 연결.
 * <ul>
 *   <li>중요 알림 2종은 alert 공개 command(RecordAlertUseCase, REQUIRED)로 호출자 트랜잭션 안에서 저장한다.
 *       저장 실패 시 거래도 rollback 된다. 비동기 CreateAlertEvent 로는 발행하지 않는다(중복 저장 방지).</li>
 *   <li>관리자 승인 대기 알림: 생성은 admin 의 CreateAdminAlertEvent(AFTER_COMMIT·async), 삭제는 admin 공개 Port(호출자 tx 참여).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TradeAlertBridge implements TradeAlertPort {

    private final RecordAlertUseCase recordAlertUseCase;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final MemberAccountQuery memberAccountQuery;
    private final RemoveAdminAlertUseCase removeAdminAlertUseCase;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void recordCompletionRequested(long productId, long buyerId, long sellerId, long receiverId) {
        String productTitle = productSnapshotQuery.get(productId).title();
        String buyerNickname = memberAccountQuery.findById(buyerId)
                .map(MemberAccount::nickname)
                .orElseThrow(NotFoundMemberException::new);
        recordAlertUseCase.record(new RecordAlertCommand(
                Alert.otherMemberTradeComplete(productId, sellerId, productTitle, buyerNickname), List.of(receiverId)));
    }

    @Override
    public void recordCancelApproved(long tradeCancelId, long requesterId, long buyerId, long sellerId) {
        recordAlertUseCase.record(new RecordAlertCommand(Alert.tradeCancel(tradeCancelId, requesterId), List.of(buyerId, sellerId)));
    }

    @Override
    public void requestCancelReview(long tradeCancelId, long requesterId) {
        applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(
                team.startup.gwangsan.domain.admin.model.AlertType.TRADE_CANCEL, tradeCancelId, requesterId));
    }

    @Override
    public void removeCancelReview(long tradeCancelId) {
        // sourceId 는 도메인마다 따로 매겨지므로 타입까지 좁혀야 다른 알림을 지우지 않는다.
        removeAdminAlertUseCase.removeBySource(
                team.startup.gwangsan.domain.admin.model.AlertType.TRADE_CANCEL, tradeCancelId);
    }
}
