package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetReportAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetSignUpAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetTradeCancelAlertResponse;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.FindAlertByAlertTypeAndPlaceService;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.presentation.dto.response.GetReportResponse;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAlertByAlertTypeAndPlaceServiceImpl implements FindAlertByAlertTypeAndPlaceService {

    private final MemberDetailRepository memberDetailRepository;
    private final ReportRepository reportRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final MemberRepository memberRepository;
    private final ReportImageRepository reportImageRepository;
    private final PlaceRepository placeRepository;
    private final MemberUtil memberUtil;
    private final ProductImageRepository productImageRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final TradeCancelImageRepository tradeCancelImageRepository;

    @Override
    @Transactional
    public GetAdminAlertResponse execute(String placeName, AlertType type) {
        Member admin = memberUtil.getCurrentMember();
        List<Place> places = findTargetPlaces(placeName, admin);

        List<AdminAlert> alerts = adminAlertRepository.findAdminAlertByPlacesAndAlertType(places, type);

        Set<Long> memberIds = alerts.stream()
                .map(alert -> alert.getRequester().getId())
                .collect(Collectors.toSet());
        Map<Long, String> memberIdToPlaceName = memberDetailRepository.findPlaceNameMapByMemberIds(memberIds);

        List<AdminAlert> reportAlerts = filterAlertsByType(alerts, AlertType.REPORT);
        List<AdminAlert> signUpAlerts = filterAlertsByType(alerts, AlertType.SIGN_UP);
        List<AdminAlert> tradeCancelAlerts = filterAlertsByType(alerts, AlertType.TRADE_CANCEL);

        List<Long> reportIds = reportAlerts.stream().map(AdminAlert::getSourceId).toList();
        List<Report> reports = reportIds.isEmpty() ? List.of() : reportRepository.findAllByIdIn(reportIds);
        List<ReportImage> reportImages = reports.isEmpty() ? List.of() : reportImageRepository.findAllByReportIdIn(reportIds);

        List<GetReportAlertResponse> reportAlertResponses =
                createReportAlertResponses(reportAlerts, reports, reportImages, memberIdToPlaceName);

        List<Long> signUpMemberIds = signUpAlerts.stream().map(AdminAlert::getSourceId).toList();
        List<Member> members = signUpMemberIds.isEmpty() ? List.of() : memberRepository.findAllByIdIn(signUpMemberIds);

        List<GetSignUpAlertResponse> signUpAlertResponses =
                createSignUpAlertResponses(signUpAlerts, members, memberIdToPlaceName);

        List<Long> tradeCancelIds = tradeCancelAlerts.stream().map(AdminAlert::getSourceId).toList();
        List<TradeCancel> tradeCancels = tradeCancelIds.isEmpty() ? List.of() : tradeCancelRepository.findAllByIdIn(tradeCancelIds);
        List<TradeCancelImage> tradeCancelImages = tradeCancels.isEmpty() ? List.of() : tradeCancelImageRepository.findByTradeCancelIn(tradeCancelIds);

        Set<Long> productIds = tradeCancels.stream()
                .map(tc -> tc.getTradeComplete().getProduct().getId())
                .collect(Collectors.toSet());

        List<ProductImage> productImages = productIds.isEmpty()
                ? List.of()
                : productImageRepository.findAllByProductIdIn(productIds);

        Map<Long, List<GetImageResponse>> productImageMap = productImages.stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(
                                pi -> new GetImageResponse(pi.getImage().getId(), pi.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        Set<Long> ownerMemberIds = tradeCancels.stream()
                .map(tc -> tc.getTradeComplete().getProduct().getMember().getId())
                .collect(Collectors.toSet());

        Map<Long, GetProductMemberResponse> ownerInfoByMemberId =
                ownerMemberIds.isEmpty()
                        ? Map.of()
                        : memberRepository.findProductMemberResponsesByMemberIds(ownerMemberIds)
                        .stream()
                        .collect(Collectors.toMap(GetProductMemberResponse::memberId, it -> it));

        List<GetTradeCancelAlertResponse> tradeCancelAlertResponses =
                createTradeCancelAlertResponses(
                        tradeCancelAlerts,
                        tradeCancels,
                        tradeCancelImages,
                        productImageMap,
                        memberIdToPlaceName,
                        ownerInfoByMemberId
                );

        return new GetAdminAlertResponse(reportAlertResponses, signUpAlertResponses, tradeCancelAlertResponses);
    }

    private List<AdminAlert> filterAlertsByType(List<AdminAlert> alerts, AlertType type) {
        return alerts.stream().filter(a -> a.getType() == type).toList();
    }

    private List<Place> findTargetPlaces(String placeName, Member member) {
        if (member.getRole() == MemberRole.ROLE_HEAD_ADMIN) {
            if (placeName == null) {
                Place place = memberDetailRepository.findPlaceByMemberId(member.getId());
                return placeRepository.findByHead(place.getHead());
            }
            Place target = placeRepository.findByName(placeName)
                    .orElseThrow(PlaceNotFoundException::new);
            return List.of(target);
        }
        Place place = memberDetailRepository.findPlaceByMemberId(member.getId());
        return List.of(place);
    }

    private List<GetReportAlertResponse> createReportAlertResponses(
            List<AdminAlert> alerts,
            List<Report> reports,
            List<ReportImage> images,
            Map<Long, String> memberIdToPlaceName
    ) {
        Map<Long, Report> reportMap = reports.stream()
                .collect(Collectors.toMap(Report::getId, r -> r));

        Map<Long, List<GetImageResponse>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getReport().getId(),
                        Collectors.mapping(ri -> new GetImageResponse(
                                ri.getImage().getId(),
                                ri.getImage().getImageUrl()), Collectors.toList())
                ));

        return alerts.stream()
                .map(alert -> {
                    Report report = Optional.ofNullable(reportMap.get(alert.getSourceId()))
                            .orElseThrow(NotFoundReportException::new);

                    List<GetImageResponse> imageResponses = imageMap.getOrDefault(report.getId(), List.of());
                    String placeName = memberIdToPlaceName.get(alert.getRequester().getId());
                    Member reported = report.getReported();

                    return new GetReportAlertResponse(
                            alert.getId(),
                            alert.getRequester().getNickname(),
                            reported != null ? reported.getId() : null,
                            reported != null ? reported.getNickname() : "알 수 없음",
                            alert.getTitle(),
                            placeName,
                            alert.getCreatedAt(),
                            new GetReportResponse(
                                    report.getId(),
                                    report.getReportType(),
                                    report.getContent(),
                                    imageResponses
                            )
                    );
                })
                .toList();
    }

    private List<GetSignUpAlertResponse> createSignUpAlertResponses(
            List<AdminAlert> alerts,
            List<Member> members,
            Map<Long, String> memberIdToPlaceName
    ) {
        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return alerts.stream()
                .map(alert -> {
                    Member member = Optional.ofNullable(memberMap.get(alert.getRequester().getId()))
                            .orElseThrow(NotFoundMemberException::new);

                    String placeName = memberIdToPlaceName.get(alert.getRequester().getId());
                    String recommenderNickname =
                            member.getRecommender() != null ? member.getRecommender().getNickname() : null;

                    return new GetSignUpAlertResponse(
                            alert.getId(),
                            member.getId(),
                            member.getNickname(),
                            alert.getTitle(),
                            placeName,
                            recommenderNickname,
                            alert.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<GetTradeCancelAlertResponse> createTradeCancelAlertResponses(
            List<AdminAlert> alerts,
            List<TradeCancel> tradeCancels,
            List<TradeCancelImage> images,
            Map<Long, List<GetImageResponse>> productImageMap,
            Map<Long, String> requesterPlaceNameMap,
            Map<Long, GetProductMemberResponse> ownerInfoByMemberId
    ) {
        Map<Long, TradeCancel> tcMap = tradeCancels.stream()
                .collect(Collectors.toMap(TradeCancel::getId, t -> t));

        Map<Long, List<GetImageResponse>> tcImageMap = images.stream()
                .collect(Collectors.groupingBy(
                        tci -> tci.getTradeCancel().getId(),
                        Collectors.mapping(
                                tci -> new GetImageResponse(tci.getImage().getId(), tci.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        return alerts.stream()
                .map(alert -> {
                    TradeCancel tc = Optional.ofNullable(tcMap.get(alert.getSourceId()))
                            .orElseThrow(NotFoundTradeCancelException::new);

                    Product product = tc.getTradeComplete().getProduct();
                    Member owner = product.getMember();
                    Long ownerId = owner.getId();

                    List<GetImageResponse> cancelImages = tcImageMap.getOrDefault(tc.getId(), List.of());
                    List<GetImageResponse> pImages = productImageMap.getOrDefault(product.getId(), List.of());

                    String requesterPlace = requesterPlaceNameMap.get(alert.getRequester().getId());

                    GetProductMemberResponse ownerInfo = ownerInfoByMemberId.get(ownerId);
                    String ownerPlace = ownerInfo.placeName();
                    Integer ownerLight = Math.max(1, ownerInfo.light() / 10);

                    GetProductResponse productDto = buildProductResponse(product, pImages, ownerPlace, ownerLight);

                    return new GetTradeCancelAlertResponse(
                            alert.getId(),
                            alert.getRequester().getNickname(),
                            alert.getTitle(),
                            tc.getReason(),
                            requesterPlace,
                            alert.getCreatedAt(),
                            cancelImages,
                            productDto
                    );
                })
                .toList();
    }

    private GetProductResponse buildProductResponse(
            Product product,
            List<GetImageResponse> images,
            String ownerPlaceName,
            Integer ownerLight
    ) {
        Member member = product.getMember();

        GetProductMemberResponse memberDto = new GetProductMemberResponse(
                member.getId(),
                member.getNickname(),
                ownerPlaceName,
                ownerLight
        );

        return new GetProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getGwangsan(),
                product.getType(),
                product.getMode(),
                memberDto,
                images
        );
    }
}