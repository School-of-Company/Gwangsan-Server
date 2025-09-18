package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.post.exception.*;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class RequestTradeCompleteServiceImpl implements RequestTradeCompleteService {

    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TradeCompleteRepository tradeCompleteRepository;

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        Member member = memberUtil.getCurrentMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        validateProductStatus(product);

        Member otherMember = memberRepository.findById(otherMemberId)
                .orElseThrow(NotFoundMemberException::new);

        boolean isBuyer = isBuyer(product, member);
        Member buyer  = isBuyer ? member    : otherMember;
        Member seller = isBuyer ? otherMember: member;

        ChatRoom chatRoom = findChatRoom(product, buyer, seller);
        validateChatExists(chatRoom, member.getId());

        if (isBuyer) handleBuyerTradeCompletion(product, buyer, seller);
        else         handleSellerTradeCompletion(product, seller, buyer);

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

    private ChatRoom findChatRoom(Product product, Member buyer, Member seller) {
        return chatRoomRepository.findByProductIdAndBuyerAndSeller(product.getId(), buyer, seller)
                .orElseThrow(NotFoundChatRoomException::new);
    }

    private void validateChatExists(ChatRoom room, Long memberId) {
        boolean hasChat = chatMessageRepository.existsByRoomAndSenderId(room, memberId);
        if (!hasChat) {
            throw new TradeCompleteWithoutChattingException();
        }
    }

    private boolean isBuyer(Product product, Member member) {
        if (product.getMode() == Mode.GIVER) {
            return !product.getMember().equals(member);
        } else {
            return product.getMember().equals(member);
        }
    }

    private void handleBuyerTradeCompletion(Product product, Member buyer, Member seller) {
        TradeComplete pending = tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING)
                .orElseThrow(SellerNotTradeCompleteException::new);

        product.updateStatus(ProductStatus.COMPLETED);
        pending.updateStatus(TradeStatus.COMPLETED);
        pending.updateCompletedAt();
    }

    private void handleSellerTradeCompletion(Product product, Member seller, Member buyer) {
        boolean existsTradeComplete = tradeCompleteRepository
                .existsByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING);

        if (existsTradeComplete) {
            throw new TradeAlreadyCompleteRequestException();
        }
        TradeComplete newTradeComplete = TradeComplete.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(TradeStatus.PENDING)
                .build();

        tradeCompleteRepository.save(newTradeComplete);
    }
}
