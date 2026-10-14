package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertJpaEntity;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.admin.data.AdminImage;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.port.out.AdminAlertReadPort;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaRepository;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelImageJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.web.post.dto.GetProductMemberResponse;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 알림 목록 조회 조합. 기존 FindAlertByAlertTypeAndPlaceServiceImpl 의 batch 조회·매핑을 그대로 옮겼다.
 * 철회 대상 상품의 값·이미지는 post(PR06) 공개 query(ProductSnapshotQuery·ProductImageQuery, 이미지 ID)를 쓴다.
 * legacy bridge: 신고 종류·신고 이미지 ID·신고 대상(PR09), 철회 요청·이미지(PR07), 가입 회원·지점명·작성자 정보(PR02)는
 * 필요한 값을 주는 공개 batch query 가 없어 각 저장 모델을 읽는다. 제거: 각 업무 공개 batch query → PR12.
 */
@Component
@RequiredArgsConstructor
public class AdminAlertReadBridge implements AdminAlertReadPort {

    private final AdminAlertPersistenceAdapter adminAlertPersistenceAdapter;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final ReportJpaRepository reportRepository;
    private final MemberJpaRepository memberRepository;
    private final ReportImageJpaRepository reportImageRepository;
    private final TradeCancelJpaRepository tradeCancelRepository;
    private final TradeCancelImageJpaRepository tradeCancelImageRepository;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;

    @Override
    public AdminAlertsResult findAlerts(List<Integer> placeIds, AlertType type) {
        List<AdminAlertJpaEntity> alerts = adminAlertPersistenceAdapter.findByPlacesAndType(placeIds, type);

        Set<Long> memberIds = alerts.stream()
                .map(alert -> alert.getRequester().getId())
                .collect(Collectors.toSet());
        Map<Long, String> memberIdToPlaceName = memberDetailRepository.findPlaceNameMapByMemberIds(memberIds);

        List<AdminAlertJpaEntity> reportAlerts = filterAlertsByType(alerts, AlertType.REPORT);
        List<AdminAlertJpaEntity> signUpAlerts = filterAlertsByType(alerts, AlertType.SIGN_UP);
        List<AdminAlertJpaEntity> tradeCancelAlerts = filterAlertsByType(alerts, AlertType.TRADE_CANCEL);

        List<Long> reportIds = reportAlerts.stream().map(AdminAlertJpaEntity::getSourceId).toList();
        List<ReportJpaEntity> reports = reportIds.isEmpty() ? List.of() : reportRepository.findAllByIdIn(reportIds);
        List<ReportImageJpaEntity> reportImages = reports.isEmpty() ? List.of() : reportImageRepository.findAllByReportIdIn(reportIds);
        List<AdminAlertsResult.ReportAlert> reportResults =
                createReportAlerts(reportAlerts, reports, reportImages, memberIdToPlaceName);

        List<Long> signUpMemberIds = signUpAlerts.stream().map(AdminAlertJpaEntity::getSourceId).toList();
        List<MemberJpaEntity> members = signUpMemberIds.isEmpty() ? List.of() : memberRepository.findAllByIdIn(signUpMemberIds);
        List<AdminAlertsResult.SignUpAlert> signUpResults =
                createSignUpAlerts(signUpAlerts, members, memberIdToPlaceName);

        List<Long> tradeCancelIds = tradeCancelAlerts.stream().map(AdminAlertJpaEntity::getSourceId).toList();
        List<TradeCancelJpaEntity> tradeCancels = tradeCancelIds.isEmpty() ? List.of() : tradeCancelRepository.findAllByIdIn(tradeCancelIds);
        List<TradeCancelImageJpaEntity> tradeCancelImages = tradeCancels.isEmpty() ? List.of() : tradeCancelImageRepository.findByTradeCancelIn(tradeCancelIds);

        Set<Long> productIds = tradeCancels.stream()
                .map(tc -> tc.getTradeComplete().getProduct().getId())
                .collect(Collectors.toSet());
        Map<Long, ProductSnapshot> productMap = productSnapshotQuery.findAll(productIds);
        Map<Long, List<AdminImage>> productImageMap = new HashMap<>();
        productImageQuery.findImages(productIds).forEach((productId, images) -> productImageMap.put(productId,
                images.stream().map(AdminAlertReadBridge::toImage).toList()));

        Set<Long> ownerMemberIds = productIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .map(ProductSnapshot::authorId)
                .collect(Collectors.toSet());
        Map<Long, GetProductMemberResponse> ownerInfoByMemberId =
                ownerMemberIds.isEmpty()
                        ? Map.of()
                        : memberRepository.findProductMemberResponsesByMemberIds(ownerMemberIds)
                        .stream()
                        .collect(Collectors.toMap(GetProductMemberResponse::memberId, it -> it));

        List<AdminAlertsResult.TradeCancelAlert> tradeCancelResults = createTradeCancelAlerts(
                tradeCancelAlerts, tradeCancels, tradeCancelImages, productMap, productImageMap, memberIdToPlaceName, ownerInfoByMemberId);

        return new AdminAlertsResult(reportResults, signUpResults, tradeCancelResults);
    }

    private List<AdminAlertJpaEntity> filterAlertsByType(List<AdminAlertJpaEntity> alerts, AlertType type) {
        return alerts.stream().filter(a -> a.getType() == type).toList();
    }

    private List<AdminAlertsResult.ReportAlert> createReportAlerts(
            List<AdminAlertJpaEntity> alerts,
            List<ReportJpaEntity> reports,
            List<ReportImageJpaEntity> images,
            Map<Long, String> memberIdToPlaceName
    ) {
        Map<Long, ReportJpaEntity> reportMap = reports.stream().collect(Collectors.toMap(ReportJpaEntity::getId, r -> r));
        Map<Long, List<AdminImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getReport().getId(),
                        Collectors.mapping(ri -> new AdminImage(ri.getImage().getId(), ri.getImage().getImageUrl()),
                                Collectors.toList())
                ));

        return alerts.stream()
                .map(alert -> {
                    ReportJpaEntity report = Optional.ofNullable(reportMap.get(alert.getSourceId()))
                            .orElseThrow(NotFoundReportException::new);
                    MemberJpaEntity reported = report.getReported();
                    Product reportedProduct = report.getProduct();
                    return new AdminAlertsResult.ReportAlert(
                            alert.getId(),
                            alert.getRequester().getNickname(),
                            reported != null ? reported.getId() : null,
                            reported != null ? reported.getNickname() : "알 수 없음",
                            reportedProduct != null,
                            reportedProduct != null ? reportedProduct.getId() : null,
                            reportedProduct != null ? reportedProduct.getTitle() : null,
                            alert.getTitle(),
                            memberIdToPlaceName.get(alert.getRequester().getId()),
                            alert.getCreatedAt(),
                            report.getId(),
                            nameOf(report.getReportType()),
                            report.getContent(),
                            imageMap.getOrDefault(report.getId(), List.of())
                    );
                })
                .toList();
    }

    private List<AdminAlertsResult.SignUpAlert> createSignUpAlerts(
            List<AdminAlertJpaEntity> alerts,
            List<MemberJpaEntity> members,
            Map<Long, String> memberIdToPlaceName
    ) {
        Map<Long, MemberJpaEntity> memberMap = members.stream().collect(Collectors.toMap(MemberJpaEntity::getId, m -> m));

        return alerts.stream()
                .map(alert -> {
                    MemberJpaEntity member = Optional.ofNullable(memberMap.get(alert.getRequester().getId()))
                            .orElseThrow(NotFoundMemberException::new);
                    String recommenderNickname =
                            member.getRecommender() != null ? member.getRecommender().getNickname() : null;
                    return new AdminAlertsResult.SignUpAlert(
                            alert.getId(),
                            member.getId(),
                            member.getNickname(),
                            alert.getTitle(),
                            memberIdToPlaceName.get(alert.getRequester().getId()),
                            recommenderNickname,
                            alert.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<AdminAlertsResult.TradeCancelAlert> createTradeCancelAlerts(
            List<AdminAlertJpaEntity> alerts,
            List<TradeCancelJpaEntity> tradeCancels,
            List<TradeCancelImageJpaEntity> images,
            Map<Long, ProductSnapshot> productMap,
            Map<Long, List<AdminImage>> productImageMap,
            Map<Long, String> requesterPlaceNameMap,
            Map<Long, GetProductMemberResponse> ownerInfoByMemberId
    ) {
        Map<Long, TradeCancelJpaEntity> tcMap = tradeCancels.stream().collect(Collectors.toMap(TradeCancelJpaEntity::getId, t -> t));
        Map<Long, List<AdminImage>> tcImageMap = images.stream()
                .collect(Collectors.groupingBy(
                        tci -> tci.getTradeCancel().getId(),
                        Collectors.mapping(tci -> new AdminImage(tci.getImage().getId(), tci.getImage().getImageUrl()),
                                Collectors.toList())
                ));

        return alerts.stream()
                .map(alert -> {
                    TradeCancelJpaEntity tc = Optional.ofNullable(tcMap.get(alert.getSourceId()))
                            .orElseThrow(NotFoundTradeCancelException::new);
                    ProductSnapshot product = productMap.get(tc.getTradeComplete().getProduct().getId());
                    GetProductMemberResponse ownerInfo = ownerInfoByMemberId.get(product.authorId());
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
                            alert.getId(),
                            alert.getRequester().getNickname(),
                            alert.getTitle(),
                            tc.getReason(),
                            requesterPlaceNameMap.get(alert.getRequester().getId()),
                            alert.getCreatedAt(),
                            tcImageMap.getOrDefault(tc.getId(), List.of()),
                            productResult
                    );
                })
                .toList();
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static AdminImage toImage(ImageView image) {
        return new AdminImage(image.imageId(), image.imageUrl());
    }
}
