package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.AlreadyTradeCancelRequestException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeCancelServiceImpl implements TradeCancelService {

    private final MemberUtil memberUtil;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final ImageRepository imageRepository;
    private final TradeCancelImageRepository tradeCancelImageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(Long productId, String reason, List<Long> imageIds) {
        Member member = memberUtil.getCurrentMember();

        TradeComplete tradeComplete = tradeCompleteRepository.findByProductIdAndStatus(
                        productId, TradeStatus.COMPLETED)
                .orElseThrow(NotFoundTradeCompleteException::new);

        if (tradeComplete.getBuyer().getId().equals(member.getId())
                && tradeComplete.getSeller().getId().equals(member.getId())) {
            throw new TradeParticipantOnlyException();
        }

        if (tradeCancelRepository.existsByTradeCompleteIdAndStatus(tradeComplete.getId(), TradeCancelStatus.PENDING)) {
            throw new AlreadyTradeCancelRequestException();
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
    }
}
