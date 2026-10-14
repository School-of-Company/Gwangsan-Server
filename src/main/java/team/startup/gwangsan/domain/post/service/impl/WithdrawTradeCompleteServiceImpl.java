package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.adapter.persistence.post.ProductRepository;
import team.startup.gwangsan.domain.post.service.WithdrawTradeCompleteService;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.NotTradeCompleteRequesterException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

@Service
@RequiredArgsConstructor
public class WithdrawTradeCompleteServiceImpl implements WithdrawTradeCompleteService {

    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TradeStateReader tradeStateReader;

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        String phoneNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberDetailJpaEntity memberDetail = memberDetailRepository.findByPhoneNumberWithMember(phoneNumber);
        MemberJpaEntity member = memberDetail.getMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(NotFoundProductException::new);
        validateProductStatus(product);

        MemberDetailJpaEntity otherMemberDetail = memberDetailRepository.findByMemberIdWithMember(otherMemberId);

        boolean isBuyer = isBuyer(product, member);
        MemberDetailJpaEntity buyerDetail = isBuyer ? memberDetail : otherMemberDetail;
        MemberDetailJpaEntity sellerDetail = isBuyer ? otherMemberDetail : memberDetail;

        MemberJpaEntity buyer = buyerDetail.getMember();
        MemberJpaEntity seller = sellerDetail.getMember();

        TradeComplete pending = tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING)
                .orElseThrow(NotFoundTradeCompleteException::new);

        boolean isRequester = isBuyer != pending.isRequestedBySeller();
        if (!isRequester) {
            throw new NotTradeCompleteRequesterException();
        }

        ChatRoom chatRoom = chatRoomRepository.findByProductIdAndBuyerAndSeller(product.getId(), buyer, seller)
                .orElseThrow(NotFoundChatRoomException::new);

        tradeCompleteRepository.delete(pending);

        // 삭제 이후 상태를 단일 지점(TradeStateReader)으로 다시 읽어, 조회 응답과 같은 값을 이벤트에 싣는다.
        TradeStateSnapshot tradeState = tradeStateReader.read(product, buyer, seller);
        applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(
                chatRoom.getId(),
                product.getId(),
                tradeState.completed(),
                tradeState.reserved(),
                tradeState.requestedBySeller(),
                tradeState.requestedAt()
        ));
    }

    private void validateNotSelfTrade(Long memberId, Long otherMemberId) {
        if (memberId.equals(otherMemberId)) {
            throw new CannotSelectSelfException();
        }
    }

    private void validateProductStatus(Product product) {
        if (product.getStatus() == ProductStatus.COMPLETED) {
            throw new TradeAlreadyCompleteException();
        }
    }

    private boolean isBuyer(Product product, MemberJpaEntity member) {
        if (product.getMode() == Mode.GIVER) {
            return !product.getMember().equals(member);
        } else {
            return product.getMember().equals(member);
        }
    }
}
