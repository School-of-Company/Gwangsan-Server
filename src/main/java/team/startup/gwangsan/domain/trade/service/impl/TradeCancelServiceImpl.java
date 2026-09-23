package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.AlreadyTradeCancelRequestException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.presentation.dto.response.TradeCancelResponse;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelApplier;
import team.startup.gwangsan.domain.trade.service.TradeCancelService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeCancelServiceImpl implements TradeCancelService {

    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final ImageRepository imageRepository;
    private final TradeCancelImageRepository tradeCancelImageRepository;
    private final TradeCancelApplier tradeCancelApplier;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * READ_COMMITTED 를 쓰는 이유: 기본값인 REPEATABLE READ 에서는 첫 SELECT 인
     * {@code getCurrentMember()} 시점에 read view 가 고정된다. 그러면 아래에서 상품 잠금을
     * 기다리는 동안 상대방 트랜잭션이 철회를 커밋해도 이 트랜잭션은 계속 옛 값을 보고,
     * 광산이 두 번 환불된다. 잠금을 잡은 뒤의 조회가 최신 커밋본을 보게 해야 한다.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TradeCancelResponse execute(Long productId, String reason, List<Long> imageIds) {
        Member member = memberUtil.getCurrentMember();

        // 철회 요청·승인·기각·요청 취소가 모두 이 잠금을 거쳐 직렬화된다.
        // 첫 요청 시점에는 TradeCancel 행이 아직 없으므로 상시 존재하는 상품 행을 잠근다.
        // 판매자가 철회 대기 중에 글을 지워도 환불이 막히면 안 되므로 삭제된 게시글도 포함한다.
        productRepository.findByIdForUpdate(productId)
                .orElseThrow(NotFoundProductException::new);

        TradeComplete tradeComplete = tradeCompleteRepository.findByProductIdAndStatus(
                        productId, TradeStatus.COMPLETED)
                .orElseThrow(NotFoundTradeCompleteException::new);

        if (!Objects.equals(tradeComplete.getBuyer().getId(), member.getId())
                && !Objects.equals(tradeComplete.getSeller().getId(), member.getId())) {
            throw new TradeParticipantOnlyException();
        }

        Optional<TradeCancel> pending = tradeCancelRepository.findByTradeCompleteIdAndStatus(
                tradeComplete.getId(), TradeCancelStatus.PENDING);

        if (pending.isPresent()) {
            TradeCancel requested = pending.get();

            if (Objects.equals(requested.getMember().getId(), member.getId())) {
                throw new AlreadyTradeCancelRequestException();
            }

            // 상대방이 같은 거래의 철회를 요청했다는 것은 양측이 동의했다는 뜻이다.
            // 두 번째 요청은 동의 신호로만 쓰고 별도 TradeCancel 로 남기지 않는다.
            tradeCancelApplier.apply(requested);
            return new TradeCancelResponse(true);
        }

        TradeCancel tradeCancel = tradeCancelRepository.save(TradeCancel.builder()
                .tradeComplete(tradeComplete)
                .member(member)
                .reason(reason)
                .status(TradeCancelStatus.PENDING)
                .build());

        List<Image> images = imageRepository.findAllById(imageIds);

        List<TradeCancelImage> tradeCancelImages = images.stream()
                .map(image -> TradeCancelImage.builder()
                        .tradeCancel(tradeCancel)
                        .image(image)
                        .build())
                .toList();

        tradeCancelImageRepository.saveAll(tradeCancelImages);

        applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(AlertType.TRADE_CANCEL, tradeCancel.getId(), member.getId()));

        return new TradeCancelResponse(false);
    }
}
