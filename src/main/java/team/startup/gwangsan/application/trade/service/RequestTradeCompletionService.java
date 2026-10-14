package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCompletionUseCase;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.TradeCompleteWithoutChattingException;
import team.startup.gwangsan.domain.trade.model.TradeComplete;
import team.startup.gwangsan.domain.trade.model.TradeProduct;
import team.startup.gwangsan.domain.trade.model.TradeSides;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTradeCompletionService implements RequestTradeCompletionUseCase {

    private final TradeBlockPort tradeBlockPort;
    private final TradeProductPort tradeProductPort;
    private final TradeMemberPort tradeMemberPort;
    private final TradeReservationPort tradeReservationPort;
    private final TradeChatPort tradeChatPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeAlertPort tradeAlertPort;
    private final TradePushPort tradePushPort;
    private final TradeStatusPort tradeStatusPort;

    /**
     * 게시글 작성자는 즉시 완료한다. 비작성자는 대기 요청이 없으면 요청을 만들고, 상대방의 대기 요청이면 확정한다.
     *
     * <p>READ_COMMITTED 를 쓰는 이유: 기본값인 REPEATABLE READ 에서는 상품 잠금 전의 첫 SELECT 시점에
     * read view 가 고정된다. 잠금을 기다리는 동안 앞선 요청이 PENDING 을 커밋해도 이 트랜잭션의
     * PENDING 조회는 옛 스냅샷을 보고 다시 INSERT 해 유니크 제약 위반(500)이 난다.
     * 잠금을 잡은 뒤의 조회가 최신 커밋본을 보게 해야 한다.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(TradeCompletionCommand command) {
        long actorId = command.actorId();
        long otherMemberId = command.otherMemberId();

        // 기존 @CheckBlocked 와 같이 다른 검증보다 먼저 차단을 확인한다.
        tradeBlockPort.validateNotBlocked(actorId, otherMemberId);
        if (actorId == otherMemberId) {
            throw new CannotSelectSelfException();
        }

        TradeProduct product = tradeProductPort.lockActive(command.productId());
        product.validateNotCompleted();
        tradeMemberPort.requireMember(otherMemberId);

        TradeSides sides = product.sides(actorId, otherMemberId);
        Long reservationId = product.reserved()
                ? tradeReservationPort.requireParticipantReservation(product.productId(), sides.buyerId(), sides.sellerId())
                : null;

        long roomId = tradeChatPort.requireRoomId(product.productId(), sides.buyerId(), sides.sellerId());
        if (!tradeChatPort.hasMessageFrom(roomId, actorId)) {
            throw new TradeCompleteWithoutChattingException();
        }

        Optional<TradeComplete> pending = tradeCompletePort.findPending(product.productId(), sides.buyerId(), sides.sellerId());
        LocalDateTime now = LocalDateTime.now();
        if (product.isOwnedBy(actorId)) {
            // 게시글 작성자는 요청자가 누구든 대기 요청을 확정하고, 대기 요청이 없으면 바로 완료 거래를 만든다.
            TradeComplete trade = pending
                    .map(p -> {
                        p.confirmByAuthor(now);
                        return p;
                    })
                    .orElseGet(() -> tradeCompletePort.create(TradeComplete.completeByAuthor(product.productId(), sides, now)));
            complete(trade, sides, roomId, product, reservationId);
        } else if (pending.isPresent()) {
            pending.get().confirm(sides.actorIsBuyer(), now);
            complete(pending.get(), sides, roomId, product, reservationId);
        } else {
            request(sides, roomId, product, otherMemberId);
        }
    }

    private void complete(TradeComplete trade, TradeSides sides, long roomId, TradeProduct product, Long reservationId) {
        tradeProductPort.complete(product.productId());
        tradeCompletePort.update(trade);
        tradeMemberPort.transfer(sides.buyerId(), sides.sellerId(), product.gwangsan());
        if (reservationId != null) {
            tradeReservationPort.completeReservation(reservationId);
        }

        tradePushPort.notifyCompleted(List.of(sides.buyerId(), sides.sellerId()), product.productId());
        tradeCompletePort.deleteOtherPending(product.productId());

        // 완료 후에는 대기 중인 요청이 없으므로 requestedBySeller 는 null 이고,
        // requestedAt 은 방금 완료된 거래의 생성 시각을 유지한다. 조회 응답과 같은 값이다.
        log.info("[TRADE-NOTIFY] publish(confirm) roomId={}, productId={}", roomId, product.productId());
        tradeStatusPort.publish(new TradeStatusChange(roomId, product.productId(), true, false, null, trade.createdAt()));
    }

    private void request(TradeSides sides, long roomId, TradeProduct product, long receiverId) {
        TradeComplete created = tradeCompletePort.create(TradeComplete.request(product.productId(), sides));

        tradeAlertPort.recordCompletionRequested(product.productId(), created.buyerId(), created.sellerId(), receiverId);

        // 저장 시 createdAt 이 채워지므로 조회 응답이 나중에 줄 값과 동일하다.
        log.info("[TRADE-NOTIFY] publish(request) roomId={}, productId={}, requestedBySeller={}",
                roomId, product.productId(), created.requestedBySeller());
        tradeStatusPort.publish(new TradeStatusChange(roomId, product.productId(), false, product.reserved(),
                created.requestedBySeller(), created.createdAt()));
    }
}
