package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.presentation.dto.response.GetTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.GetTradeProductResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.constant.TradeRole;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.FindMyTradeHistoryService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindMyTradeHistoryServiceImpl implements FindMyTradeHistoryService {

    private final TradeCompleteRepository tradeCompleteRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<GetTradeHistoryResponse> execute(TradeStatus status) {
        Member member = memberUtil.getCurrentMember();

        List<TradeComplete> trades = tradeCompleteRepository.findAllByMemberAndStatus(member, status);

        if (trades.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = trades.stream()
                .map(trade -> trade.getProduct().getId())
                .distinct()
                .toList();

        // ponytail: ProductImage에 순서 컬럼이 없어 조회 순서상 첫 장을 대표로 쓴다.
        Map<Long, GetImageResponse> imageMap = productImageRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        pi -> pi.getProduct().getId(),
                        pi -> new GetImageResponse(pi.getImage().getId(), pi.getImage().getImageUrl()),
                        (first, second) -> first
                ));

        return trades.stream()
                .map(trade -> {
                    Product product = trade.getProduct();
                    TradeRole role = trade.getBuyer().getId().equals(member.getId())
                            ? TradeRole.BUYER
                            : TradeRole.SELLER;

                    return new GetTradeHistoryResponse(
                            trade.getId(),
                            role,
                            trade.getStatus(),
                            trade.getCompletedAt(),
                            new GetTradeProductResponse(
                                    product.getId(),
                                    product.getTitle(),
                                    product.getGwangsan(),
                                    imageMap.get(product.getId())
                            )
                    );
                })
                .toList();
    }
}
