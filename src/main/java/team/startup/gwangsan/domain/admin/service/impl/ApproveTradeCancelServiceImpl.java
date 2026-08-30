package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.ApproveTradeCancelService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.util.MemberUtil;


@Service
@RequiredArgsConstructor
public class ApproveTradeCancelServiceImpl implements ApproveTradeCancelService {

    private final AdminAlertRepository adminAlertRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final ChatRoomRepository chatRoomRepository;
    private final TradeStateReader tradeStateReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        AdminAlert alert = adminAlertRepository.findByIdWithMember(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetail adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        TradeCancel tradeCancel = tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(alert.getSourceId())
                .orElseThrow(NotFoundTradeCancelException::new);

        if (tradeCancel.getStatus() != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }

        TradeComplete tradeComplete = tradeCancel.getTradeComplete();
        Product product = tradeComplete.getProduct();
        Member buyer = tradeComplete.getBuyer();
        Member seller = tradeComplete.getSeller();

        MemberDetail buyerDetail = memberDetailRepository.findById(buyer.getId())
                .orElseThrow(NotFoundMemberDetailException::new);
        MemberDetail sellerDetail = memberDetailRepository.findById(seller.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, buyerDetail);
        validatePlaceUtil.validateSamePlace(admin, adminDetail, sellerDetail);

        int gwangsan = product.getGwangsan();

        buyerDetail.plusGwangsan(gwangsan);
        sellerDetail.minusGwangsan(gwangsan);

        tradeCancel.updateStatus(TradeCancelStatus.APPROVED);

        tradeComplete.updateStatus(TradeStatus.ROLLED_BACK);

        // 상품을 다시 거래 가능 상태로 되돌린다. 이걸 빠뜨리면 채팅방의 isCompleted 가
        // Product.status 에서만 파생되므로 철회 후에도 영구히 "거래 완료"로 남는다.
        product.updateStatus(ProductStatus.ONGOING);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                tradeCancel.getId(),
                alert.getRequester().getId(),
                AlertType.TRADE_CANCEL
        ));

        // 철회가 승인되면 대기 중인 요청도 완료된 요청도 남지 않으므로
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
}
