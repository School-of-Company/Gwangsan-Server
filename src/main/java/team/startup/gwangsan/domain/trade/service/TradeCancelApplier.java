package team.startup.gwangsan.domain.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

/**
 * 거래 철회를 실제로 반영하는 단일 지점.
 *
 * <p>관리자 승인 경로와 양측 동의 자동 철회 경로가 이 클래스를 공유한다. 두 경로가 잔액과
 * 상태를 각자 바꾸면 결과가 갈라지므로, 관리자 인가({@code ValidatePlaceUtil})만 호출자에 남기고
 * 나머지 상태 전이는 전부 여기로 모았다.
 */
@Component
@RequiredArgsConstructor
public class TradeCancelApplier {

    private final MemberDetailRepository memberDetailRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final TradeStateReader tradeStateReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 호출 전제: 호출자가 이미 해당 상품 행을 비관적 잠금으로 확보했고, {@code tradeCancel} 을
     * 그 잠금 이후에 처음 읽었다. 잠금 전에 읽은 엔티티를 넘기면 아래 PENDING 검사가 옛 값을 보고
     * 통과해 광산이 두 번 환불된다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(TradeCancel tradeCancel) {
        if (tradeCancel.getStatus() != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }

        TradeComplete tradeComplete = tradeCancel.getTradeComplete();
        Product product = tradeComplete.getProduct();
        Member buyer = tradeComplete.getBuyer();
        Member seller = tradeComplete.getSeller();

        MemberDetail buyerDetail = getMemberDetail(buyer.getId());
        MemberDetail sellerDetail = getMemberDetail(seller.getId());

        int gwangsan = product.getGwangsan();

        buyerDetail.plusGwangsan(gwangsan);
        sellerDetail.minusGwangsan(gwangsan);

        tradeCancel.updateStatus(TradeCancelStatus.APPROVED);

        tradeComplete.updateStatus(TradeStatus.ROLLED_BACK);

        // 상품을 다시 거래 가능 상태로 되돌린다. 이걸 빠뜨리면 채팅방의 isCompleted 가
        // Product.status 에서만 파생되므로 철회 후에도 영구히 "거래 완료"로 남는다.
        product.updateStatus(ProductStatus.ONGOING);

        // 처리된 요청이 관리자 목록에 남으면 승인도 기각도 되지 않는 유령 항목이 된다.
        // sourceId 는 도메인마다 따로 매겨지므로 타입까지 좁혀야 다른 알림을 지우지 않는다.
        adminAlertRepository.findByTypeAndSourceId(
                        team.startup.gwangsan.domain.admin.entity.constant.AlertType.TRADE_CANCEL,
                        tradeCancel.getId())
                .ifPresent(adminAlertRepository::delete);

        // CreateAlertServiceImpl 의 TRADE_CANCEL 분기가 buyer 와 seller 양쪽에 AlertReceipt 를
        // 만든다. memberId 는 수신자가 아니라 존재 검증용이라 한 건만 발행하면 된다.
        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                tradeCancel.getId(),
                tradeCancel.getMember().getId(),
                AlertType.TRADE_CANCEL
        ));

        // 철회가 반영되면 대기 중인 요청도 완료된 요청도 남지 않으므로
        // requestedBySeller 와 requestedAt 이 모두 null 이 된다. 그래야 클라이언트가
        // 거래 카드를 감추고 재요청 버튼을 다시 열어 준다.
        chatRoomRepository.findByProductIdAndBuyerAndSeller(product.getId(), buyer, seller)
                .ifPresent(chatRoom -> {
                    TradeStateSnapshot tradeState = tradeStateReader.read(product, buyer, seller);
                    applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(
                            chatRoom.getId(),
                            product.getId(),
                            tradeState.completed(),
                            tradeState.reserved(),
                            tradeState.requestedBySeller(),
                            tradeState.requestedAt()
                    ));
                });
    }

    private MemberDetail getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }
}
