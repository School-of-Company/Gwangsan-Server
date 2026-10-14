package team.startup.gwangsan.adapter.integration.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.port.out.LoadAlertImagesPort;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 임시 bridge: 알림 이미지 조회를 기존 Repository 쿼리 그대로 수행한다.
 * 공지(PR10)·상품(PR06)·신고(PR09)·거래 철회(PR07)의 공개 조회로 교체하고 PR12 에서 최종 확인한다.
 */
@Component
@RequiredArgsConstructor
public class LegacyAlertImageAdapter implements LoadAlertImagesPort {

    private final NoticeImageRepository noticeImageRepository;
    private final ProductImageRepository productImageRepository;
    private final ReportImageRepository reportImageRepository;
    private final TradeCancelRepository tradeCancelRepository;

    @Override
    public Map<Long, List<AlertImage>> noticeImages(List<Long> noticeIds) {
        return noticeImageRepository.findAllByNoticeIdIn(noticeIds).stream()
                .collect(Collectors.groupingBy(
                        ni -> ni.getNotice().getId(),
                        Collectors.mapping(ni -> new AlertImage(ni.getId(), ni.getImage().getImageUrl()), Collectors.toList())
                ));
    }

    @Override
    public Map<Long, List<AlertImage>> productImages(List<Long> productIds) {
        return productImageRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(pi -> new AlertImage(pi.getId(), pi.getImage().getImageUrl()), Collectors.toList())
                ));
    }

    @Override
    public Map<Long, List<AlertImage>> reportImages(List<Long> reportIds) {
        return reportImageRepository.findAllByReportIdIn(reportIds).stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getReport().getId(),
                        Collectors.mapping(ri -> new AlertImage(ri.getId(), ri.getImage().getImageUrl()), Collectors.toList())
                ));
    }

    @Override
    public Map<Long, Long> productIdsByTradeCancel(List<Long> tradeCancelIds) {
        return tradeCancelRepository.findAllById(tradeCancelIds).stream()
                .filter(tc -> tc.getTradeComplete() != null && tc.getTradeComplete().getProduct() != null)
                .collect(Collectors.toMap(TradeCancel::getId, tc -> tc.getTradeComplete().getProduct().getId()));
    }
}
