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
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.CannotCompleteTradeException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
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

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() == ProductStatus.COMPLETED) {
            throw new CannotCompleteTradeException();
        }

        Member productOwner = product.getMember();

        Member otherMember;
        if (member.equals(productOwner)) {
            otherMember = memberRepository.findById(otherMemberId)
                    .orElseThrow(NotFoundMemberException::new);
        } else {
            otherMember = member;
        }

        boolean hasChat = chatMessageRepository.existsByProductIdAndSenderId(productId, otherMember.getId());
        if (!hasChat) {
            throw new CannotCompleteTradeException();
        }

        applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(
                AlertType.TRADE_COMPLETE,
                productId,
                otherMember,
                member
        ));
    }
}
