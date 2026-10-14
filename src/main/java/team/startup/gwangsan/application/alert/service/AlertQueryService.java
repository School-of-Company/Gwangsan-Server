package team.startup.gwangsan.application.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.data.AlertView;
import team.startup.gwangsan.application.alert.port.in.MyAlertsQuery;
import team.startup.gwangsan.application.alert.port.in.UnreadAlertQuery;
import team.startup.gwangsan.application.alert.port.out.LoadAlertImagesPort;
import team.startup.gwangsan.application.alert.port.out.LoadAlertPort;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.alert.model.AlertType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertQueryService implements MyAlertsQuery, UnreadAlertQuery {

    private static final Set<AlertType> PRODUCT_TYPES = EnumSet.of(
            AlertType.TRADE_COMPLETE, AlertType.TRADE_COMPLETE_REJECT, AlertType.OTHER_MEMBER_TRADE_COMPLETE);
    private static final Set<AlertType> TRADE_CANCEL_TYPES = EnumSet.of(AlertType.TRADE_CANCEL, AlertType.TRADE_CANCEL_REJECT);
    private static final Set<AlertType> REPORT_TYPES = EnumSet.of(AlertType.REPORT, AlertType.REPORT_REJECT);

    private final LoadAlertPort loadAlertPort;
    private final LoadAlertImagesPort loadAlertImagesPort;

    @Override
    @Transactional(readOnly = true)
    public List<AlertView> query(Long memberId) {
        List<Alert> alerts = loadAlertPort.loadReceived(memberId);

        List<Long> noticeIds = sourceIds(alerts, EnumSet.of(AlertType.NOTICE));
        List<Long> reportIds = sourceIds(alerts, REPORT_TYPES);
        List<Long> tradeCancelIds = sourceIds(alerts, TRADE_CANCEL_TYPES);

        Map<Long, Long> tradeCancelToProduct = tradeCancelIds.isEmpty()
                ? Map.of()
                : loadAlertImagesPort.productIdsByTradeCancel(tradeCancelIds);

        Set<Long> productIds = new HashSet<>(sourceIds(alerts, PRODUCT_TYPES));
        productIds.addAll(tradeCancelToProduct.values());

        // 기존 조회 횟수를 유지한다: 공지 이미지는 항상, 나머지는 대상이 있을 때만 조회한다.
        Map<Long, List<AlertImage>> noticeImages = loadAlertImagesPort.noticeImages(noticeIds);
        Map<Long, List<AlertImage>> productImages = productIds.isEmpty()
                ? Map.of()
                : loadAlertImagesPort.productImages(List.copyOf(productIds));
        Map<Long, List<AlertImage>> reportImages = reportIds.isEmpty()
                ? Map.of()
                : loadAlertImagesPort.reportImages(reportIds);

        return alerts.stream()
                .map(alert -> new AlertView(
                        alert.id(),
                        alert.title(),
                        alert.content(),
                        alert.type(),
                        alert.createdAt(),
                        images(alert, noticeImages, productImages, reportImages, tradeCancelToProduct),
                        alert.senderMemberId(),
                        alert.sourceId()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnread(Long memberId) {
        return loadAlertPort.existsUnread(memberId);
    }

    private List<AlertImage> images(Alert alert,
                                    Map<Long, List<AlertImage>> noticeImages,
                                    Map<Long, List<AlertImage>> productImages,
                                    Map<Long, List<AlertImage>> reportImages,
                                    Map<Long, Long> tradeCancelToProduct) {
        AlertType type = alert.type();
        if (type == AlertType.NOTICE) return noticeImages.getOrDefault(alert.sourceId(), List.of());
        if (PRODUCT_TYPES.contains(type)) return productImages.getOrDefault(alert.sourceId(), List.of());
        if (REPORT_TYPES.contains(type)) return reportImages.getOrDefault(alert.sourceId(), List.of());
        if (TRADE_CANCEL_TYPES.contains(type)) {
            Long productId = tradeCancelToProduct.get(alert.sourceId());
            return productId == null ? List.of() : productImages.getOrDefault(productId, List.of());
        }
        return List.of();
    }

    private static List<Long> sourceIds(List<Alert> alerts, Set<AlertType> types) {
        return alerts.stream()
                .filter(a -> types.contains(a.type()))
                .map(Alert::sourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
