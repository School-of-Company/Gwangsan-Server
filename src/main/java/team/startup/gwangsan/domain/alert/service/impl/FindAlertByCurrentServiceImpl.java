package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.service.FindAlertByCurrentService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAlertByCurrentServiceImpl implements FindAlertByCurrentService {

    private final MemberUtil memberUtil;
    private final AlertReceiptRepository alertReceiptRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final ProductImageRepository productImageRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final ReportImageRepository reportImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetAlertResponse> execute() {
        Member member = memberUtil.getCurrentMember();
        List<Alert> alerts = alertReceiptRepository.findByMemberId(member.getId());

        List<Long> noticeIds = alerts.stream()
                .filter(a -> a.getAlertType() == AlertType.NOTICE)
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> reportIds = alerts.stream()
                .filter(a -> a.getAlertType() == AlertType.REPORT || a.getAlertType() == AlertType.REPORT_REJECT)
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<AlertType> productDirectTypes = EnumSet.of(
                AlertType.TRADE_COMPLETE,
                AlertType.TRADE_COMPLETE_REJECT,
                AlertType.OTHER_MEMBER_TRADE_COMPLETE
        );

        List<Long> productIdsDirect = alerts.stream()
                .filter(a -> productDirectTypes.contains(a.getAlertType()))
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<AlertType> tradeCancelTypes = EnumSet.of(
                AlertType.TRADE_CANCEL,
                AlertType.TRADE_CANCEL_REJECT
        );

        List<Long> tradeCancelIds = alerts.stream()
                .filter(a -> tradeCancelTypes.contains(a.getAlertType()))
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Long> tradeCancelToProductIdTemp = new HashMap<>();
        List<Long> productIdsFromTradeCancel = List.of();


        if (!tradeCancelIds.isEmpty()) {
            List<TradeCancel> tradeCancels = tradeCancelRepository.findAllById(tradeCancelIds);
            tradeCancelToProductIdTemp = tradeCancels.stream()
                    .filter(tc -> tc.getTradeComplete() != null && tc.getTradeComplete().getProduct() != null)
                    .collect(Collectors.toMap(
                            TradeCancel::getId,
                            tc -> tc.getTradeComplete().getProduct().getId()
                    ));
            productIdsFromTradeCancel = new ArrayList<>(new HashSet<>(tradeCancelToProductIdTemp.values()));
        }
        final Map<Long, Long> tradeCancelToProductId = tradeCancelToProductIdTemp;

        Set<Long> allProductIds = new HashSet<>(productIdsDirect);
        allProductIds.addAll(productIdsFromTradeCancel);

        Map<Long, List<GetImageResponse>> noticeImageMap = noticeImageRepository.findAllByNoticeIdIn(noticeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ni -> ni.getNotice().getId(),
                        Collectors.mapping(
                                ni -> new GetImageResponse(ni.getId(), ni.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        Map<Long, List<GetImageResponse>> productImageMap = allProductIds.isEmpty()
                ? Map.of()
                : productImageRepository.findAllByProductIdIn(allProductIds.stream().toList())
                .stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(
                                pi -> new GetImageResponse(pi.getId(), pi.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        Map<Long, List<GetImageResponse>> reportImageMap = reportIds.isEmpty()
                ? Map.of()
                : reportImageRepository.findAllByReportIdIn(reportIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getReport().getId(),
                        Collectors.mapping(
                                ri -> new GetImageResponse(ri.getId(), ri.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        return alerts.stream()
                .map(alert -> {
                    List<GetImageResponse> images;
                    switch (alert.getAlertType()) {
                        case NOTICE -> images = noticeImageMap.getOrDefault(alert.getSourceId(), List.of());
                        case TRADE_COMPLETE, TRADE_COMPLETE_REJECT, OTHER_MEMBER_TRADE_COMPLETE ->
                                images = productImageMap.getOrDefault(alert.getSourceId(), List.of());
                        case TRADE_CANCEL, TRADE_CANCEL_REJECT -> {
                            Long productId = tradeCancelToProductId.get(alert.getSourceId());
                            images = (productId == null)
                                    ? List.of()
                                    : productImageMap.getOrDefault(productId, List.of());
                        }
                        case REPORT, REPORT_REJECT -> // ✅ 둘 다 reportImageMap 참조
                                images = reportImageMap.getOrDefault(alert.getSourceId(), List.of());
                        default -> images = List.of();
                    }

                    return new GetAlertResponse(
                            alert.getId(),
                            alert.getTitle(),
                            alert.getContent(),
                            alert.getAlertType(),
                            alert.getCreatedAt(),
                            images,
                            alert.getSendMember() != null ? alert.getSendMember().getId() : null,
                            alert.getSourceId()
                    );
                })
                .toList();
    }
}