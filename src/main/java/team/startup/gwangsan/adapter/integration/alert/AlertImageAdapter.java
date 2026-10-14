package team.startup.gwangsan.adapter.integration.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.port.out.LoadAlertImagesPort;
import team.startup.gwangsan.application.notice.port.in.NoticeQuery;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 이미지·거래 철회 상품을 각 업무 공개 조회(NoticeQuery·ProductImageQuery·ReportQuery·TradeCancelQuery)로 읽는다.
 */
@Component
@RequiredArgsConstructor
public class AlertImageAdapter implements LoadAlertImagesPort {

    private final NoticeQuery noticeQuery;
    private final ProductImageQuery productImageQuery;
    private final ReportQuery reportQuery;
    private final TradeCancelQuery tradeCancelQuery;

    @Override
    public Map<Long, List<AlertImage>> noticeImages(List<Long> noticeIds) {
        // 알림 응답은 기존부터 이미지 ID 가 아니라 공지-이미지 연결 행 ID 를 쓴다.
        return noticeQuery.findImageLinks(noticeIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(link -> new AlertImage(link.linkId(), link.imageUrl())).toList()));
    }

    @Override
    public Map<Long, List<AlertImage>> productImages(List<Long> productIds) {
        // 알림 응답은 기존부터 이미지 ID 가 아니라 게시글-이미지 연결 행 ID 를 쓴다.
        return productImageQuery.findImageLinks(productIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(link -> new AlertImage(link.linkId(), link.imageUrl())).toList()));
    }

    @Override
    public Map<Long, List<AlertImage>> reportImages(List<Long> reportIds) {
        // 알림 응답은 기존부터 이미지 ID 가 아니라 신고-이미지 연결 행 ID 를 쓴다.
        return reportQuery.findImageLinks(reportIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(link -> new AlertImage(link.linkId(), link.imageUrl())).toList()));
    }

    @Override
    public Map<Long, Long> productIdsByTradeCancel(List<Long> tradeCancelIds) {
        // 거래 철회 → 상품 ID 는 trade 공개 query(PR07)로 조회한다.
        return tradeCancelQuery.findProductIds(tradeCancelIds);
    }
}
