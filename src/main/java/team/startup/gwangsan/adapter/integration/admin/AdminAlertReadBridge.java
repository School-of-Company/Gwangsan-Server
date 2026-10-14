package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter.AlertRow;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.admin.data.AdminImage;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.port.out.AdminAlertReadPort;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.report.data.ReportDetail;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 알림 목록 조회 조합. 기존 FindAlertByAlertTypeAndPlaceServiceImpl 의 batch 조회·매핑 의미를 유지한다.
 * 알림 행은 admin 저장 adapter(요청자 fetch join), 신고는 report(PR09) ReportQuery.findDetails(이미지 ID),
 * 철회 요청·첨부는 trade(PR07) TradeCancelQuery, 철회 대상 상품 값·이미지는 post(PR06) 공개 query,
 * 회원 지점명·가입 회원·작성자 정보는 member(PR02) MemberSummaryQuery 를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class AdminAlertReadBridge implements AdminAlertReadPort {

    private final AdminAlertPersistenceAdapter adminAlertPersistenceAdapter;
    private final MemberSummaryQuery memberSummaryQuery;
    private final ReportQuery reportQuery;
    private final TradeCancelQuery tradeCancelQuery;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;

    @Override
    public AdminAlertsResult findAlerts(List<Integer> placeIds, AlertType type) {
        List<AlertRow> alerts = adminAlertPersistenceAdapter.findByPlacesAndType(placeIds, type);

        Set<Long> requesterIds = alerts.stream().map(row -> row.alert().requesterId()).collect(Collectors.toSet());
        Map<Long, String> memberIdToPlaceName = new HashMap<>();
        memberSummaryQuery.findAllByIds(requesterIds).stream()
                .filter(MemberSummary::hasDetail)
                .forEach(member -> memberIdToPlaceName.put(member.memberId(), member.placeName()));

        List<AlertRow> reportAlerts = filterAlertsByType(alerts, AlertType.REPORT);
        List<AlertRow> signUpAlerts = filterAlertsByType(alerts, AlertType.SIGN_UP);
        List<AlertRow> tradeCancelAlerts = filterAlertsByType(alerts, AlertType.TRADE_CANCEL);

        List<Long> reportIds = sourceIds(reportAlerts);
        Map<Long, ReportDetail> reports = reportIds.isEmpty() ? Map.of() : reportQuery.findDetails(reportIds);
        List<AdminAlertsResult.ReportAlert> reportResults = createReportAlerts(reportAlerts, reports, memberIdToPlaceName);

        List<Long> signUpMemberIds = sourceIds(signUpAlerts);
        Map<Long, MemberSummary> signUpMembers = signUpMemberIds.isEmpty() ? Map.of()
                : memberSummaryQuery.findAllByIds(signUpMemberIds).stream()
                .collect(Collectors.toMap(MemberSummary::memberId, Function.identity()));
        List<AdminAlertsResult.SignUpAlert> signUpResults = createSignUpAlerts(signUpAlerts, signUpMembers, memberIdToPlaceName);

        List<Long> tradeCancelIds = sourceIds(tradeCancelAlerts);
        Map<Long, TradeCancelView> tradeCancels = tradeCancelIds.isEmpty() ? Map.of()
                : tradeCancelQuery.findAll(tradeCancelIds).stream()
                .collect(Collectors.toMap(TradeCancelView::tradeCancelId, Function.identity()));
        Map<Long, List<AdminImage>> tradeCancelImages = new HashMap<>();
        if (!tradeCancels.isEmpty()) {
            tradeCancelQuery.findImages(tradeCancelIds).forEach((id, images) -> tradeCancelImages.put(id,
                    images.stream().map(image -> new AdminImage(image.imageId(), image.imageUrl())).toList()));
        }

        Set<Long> productIds = tradeCancels.values().stream().map(TradeCancelView::productId).collect(Collectors.toSet());
        Map<Long, ProductSnapshot> productMap = productSnapshotQuery.findAll(productIds);
        Map<Long, List<AdminImage>> productImageMap = new HashMap<>();
        productImageQuery.findImages(productIds).forEach((productId, images) -> productImageMap.put(productId,
                images.stream().map(image -> new AdminImage(image.imageId(), image.imageUrl())).toList()));

        Set<Long> ownerMemberIds = productMap.values().stream().map(ProductSnapshot::authorId).collect(Collectors.toSet());
        // 기존 작성자 projection 은 상세 기준(inner join)이었다.
        Map<Long, MemberSummary> ownerInfoByMemberId = ownerMemberIds.isEmpty() ? Map.of()
                : memberSummaryQuery.findAllByIds(ownerMemberIds).stream()
                .filter(MemberSummary::hasDetail)
                .collect(Collectors.toMap(MemberSummary::memberId, Function.identity()));

        List<AdminAlertsResult.TradeCancelAlert> tradeCancelResults = createTradeCancelAlerts(tradeCancelAlerts,
                tradeCancels, tradeCancelImages, productMap, productImageMap, memberIdToPlaceName, ownerInfoByMemberId);

        return new AdminAlertsResult(reportResults, signUpResults, tradeCancelResults);
    }

    private static List<AlertRow> filterAlertsByType(List<AlertRow> alerts, AlertType type) {
        return alerts.stream().filter(row -> row.alert().type() == type).toList();
    }

    private static List<Long> sourceIds(List<AlertRow> alerts) {
        return alerts.stream().map(row -> row.alert().sourceId()).toList();
    }

    private List<AdminAlertsResult.ReportAlert> createReportAlerts(
            List<AlertRow> alerts,
            Map<Long, ReportDetail> reports,
            Map<Long, String> memberIdToPlaceName
    ) {
        return alerts.stream()
                .map(row -> {
                    ReportDetail report = Optional.ofNullable(reports.get(row.alert().sourceId()))
                            .orElseThrow(NotFoundReportException::new);
                    boolean hasReported = report.reportedId() != null;
                    boolean productTarget = report.productId() != null;
                    return new AdminAlertsResult.ReportAlert(
                            row.alert().id(),
                            row.requesterNickname(),
                            hasReported ? report.reportedId() : null,
                            hasReported ? report.reportedNickname() : "알 수 없음",
                            productTarget,
                            productTarget ? report.productId() : null,
                            productTarget ? report.productTitle() : null,
                            row.alert().title(),
                            memberIdToPlaceName.get(row.alert().requesterId()),
                            row.alert().createdAt(),
                            report.reportId(),
                            nameOf(report.reportType()),
                            report.content(),
                            report.images().stream().map(image -> new AdminImage(image.imageId(), image.imageUrl())).toList()
                    );
                })
                .toList();
    }

    private List<AdminAlertsResult.SignUpAlert> createSignUpAlerts(
            List<AlertRow> alerts,
            Map<Long, MemberSummary> members,
            Map<Long, String> memberIdToPlaceName
    ) {
        return alerts.stream()
                .map(row -> {
                    MemberSummary member = Optional.ofNullable(members.get(row.alert().requesterId()))
                            .orElseThrow(NotFoundMemberException::new);
                    return new AdminAlertsResult.SignUpAlert(
                            row.alert().id(),
                            member.memberId(),
                            member.nickname(),
                            row.alert().title(),
                            memberIdToPlaceName.get(row.alert().requesterId()),
                            member.recommenderNickname(),
                            row.alert().createdAt()
                    );
                })
                .toList();
    }

    private List<AdminAlertsResult.TradeCancelAlert> createTradeCancelAlerts(
            List<AlertRow> alerts,
            Map<Long, TradeCancelView> tradeCancels,
            Map<Long, List<AdminImage>> tradeCancelImages,
            Map<Long, ProductSnapshot> productMap,
            Map<Long, List<AdminImage>> productImageMap,
            Map<Long, String> requesterPlaceNameMap,
            Map<Long, MemberSummary> ownerInfoByMemberId
    ) {
        return alerts.stream()
                .map(row -> {
                    TradeCancelView tc = Optional.ofNullable(tradeCancels.get(row.alert().sourceId()))
                            .orElseThrow(NotFoundTradeCancelException::new);
                    ProductSnapshot product = productMap.get(tc.productId());
                    MemberSummary ownerInfo = ownerInfoByMemberId.get(product.authorId());
                    AdminProduct productResult = new AdminProduct(
                            product.id(),
                            product.title(),
                            product.description(),
                            product.gwangsan(),
                            nameOf(product.type()),
                            nameOf(product.mode()),
                            new AdminProduct.Owner(product.authorId(), ownerInfo.nickname(), ownerInfo.placeName(),
                                    Math.max(1, ownerInfo.light() / 10)),
                            productImageMap.getOrDefault(product.id(), List.of()),
                            product.isCompleted(),
                            product.isReserved()
                    );
                    return new AdminAlertsResult.TradeCancelAlert(
                            row.alert().id(),
                            row.requesterNickname(),
                            row.alert().title(),
                            tc.reason(),
                            requesterPlaceNameMap.get(row.alert().requesterId()),
                            row.alert().createdAt(),
                            tradeCancelImages.getOrDefault(tc.tradeCancelId(), List.of()),
                            productResult
                    );
                })
                .toList();
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
