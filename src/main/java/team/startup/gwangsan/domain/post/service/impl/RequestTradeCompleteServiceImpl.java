package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.CannotCompleteTradeException;
import team.startup.gwangsan.domain.post.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class RequestTradeCompleteServiceImpl implements RequestTradeCompleteService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TradeCompleteRepository tradeCompleteRepository;

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        Member member = memberUtil.getCurrentMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = findProductById(productId);
        validateProductStatus(product);

        Member otherMember = findMemberById(otherMemberId);

        validateChatExists(productId, otherMember.getId());
        validateNotAlreadyRequested(product, member, otherMember);

        saveTradeComplete(product, member, otherMember);

        notifyIfMutualComplete(productId, member, otherMember);
    }

    private void validateNotSelfTrade(Long memberId, Long otherMemberId) {
        if (memberId.equals(otherMemberId)) {
            throw new CannotSelectSelfException();
        }
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
    }

    private void validateProductStatus(Product product) {
        if (product.getStatus() == ProductStatus.COMPLETED) {
            throw new CannotCompleteTradeException();
        }
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
    }

    private void validateChatExists(Long productId, Long otherMemberId) {
        boolean hasChat = chatMessageRepository.existsByProductIdAndSenderId(productId, otherMemberId);
        if (!hasChat) {
            throw new CannotCompleteTradeException();
        }
    }

    private void validateNotAlreadyRequested(Product product, Member member, Member otherMember) {
        boolean alreadyRequested = tradeCompleteRepository.existsByProductAndMemberAndOtherMember(
                product, member, otherMember
        );
        if (alreadyRequested) {
            throw new CannotCompleteTradeException();
        }
    }

    private void saveTradeComplete(Product product, Member member, Member otherMember) {
        tradeCompleteRepository.save(TradeComplete.builder()
                .product(product)
                .member(member)
                .otherMember(otherMember)
                .build());
    }

    private void notifyIfMutualComplete(Long productId, Member member, Member otherMember) {
        long count = tradeCompleteRepository.countMutualTradeComplete(
                productId, member.getId(), otherMember.getId());

        if (count == 2) {
            applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(
                    AlertType.TRADE_COMPLETE,
                    productId,
                    otherMember,
                    member
            ));
        }
    }
}
