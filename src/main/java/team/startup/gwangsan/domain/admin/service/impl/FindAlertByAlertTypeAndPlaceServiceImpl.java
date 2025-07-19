package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetReportAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetSignUpAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetTradeCompleteAlertResponse;
import team.startup.gwangsan.domain.admin.repository.custom.AdminAlertCustomRepository;
import team.startup.gwangsan.domain.admin.service.FindAlertByAlertTypeAndPlaceService;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.custom.MemberCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.presentation.dto.response.GetReportResponse;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.custom.ReportCustomRepository;
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
    private final ReportCustomRepository reportCustomRepository;
    private final AdminAlertCustomRepository adminAlertCustomRepository;
    private final MemberCustomRepository memberCustomRepository;
    private final ReportImageRepository reportImageRepository;
    private final PlaceRepository placeRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public GetAdminAlertResponse execute(String placeName, AlertType type) {
        Member admin = memberUtil.getCurrentMember();
        List<Place> places = findTargetPlaces(placeName, admin);

        List<AdminAlert> alerts = adminAlertCustomRepository.findAdminAlertByPlacesAndAlertType(places, type);

        Set<Long> memberIds = alerts.stream()
                .map(alert -> alert.getRequester().getId())
                .collect(Collectors.toSet());
        Map<Long, String> memberIdToPlaceName = memberDetailRepository.findPlaceNameMapByMemberIds(memberIds);

        List<AdminAlert> reportAlerts = filterAlertsByType(alerts, AlertType.REPORT);
        List<AdminAlert> signUpAlerts = filterAlertsByType(alerts, AlertType.SIGN_UP);
        List<AdminAlert> tradeAlerts = filterAlertsByType(alerts, AlertType.TRADE_COMPLETE);

        List<Long> productIds = tradeAlerts.stream()
                .map(AdminAlert::getSourceId)
                .toList();

        List<Report> reports = reportCustomRepository.findByPlaces(places);
        List<Member> members = memberCustomRepository.findByStatusAndPlaces(MemberStatus.PENDING, places);
        List<ReportImage> reportImages = reportImageRepository.findByReportIn(reports);
        List<Product> products = productRepository.findAllById(productIds);
        List<ProductImage> productImages = productImageRepository.findProductImageByProductIdIn(productIds);
        List<MemberDetail> memberDetails = memberDetailRepository.findAllByMemberIdIn(tradeAlerts.stream()
                .map(alert -> alert.getRequester().getId())
                .toList());

        List<GetReportAlertResponse> reportAlertResponses =
                createReportAlertResponses(reportAlerts, reports, reportImages, memberIdToPlaceName);
        List<GetSignUpAlertResponse> signUpAlertResponses =
                createSignUpAlertResponses(signUpAlerts, members, memberIdToPlaceName);
        List<GetTradeCompleteAlertResponse> tradeAlertResponses =
                createTradeCompleteAlertResponses(memberDetails, tradeAlerts, products, productImages, memberIdToPlaceName);

        return new GetAdminAlertResponse(reportAlertResponses, signUpAlertResponses, tradeAlertResponses);
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

                    return new GetReportAlertResponse(
                            report.getId(),
                            alert.getRequester().getNickname(),
                            report.getReported().getId(),
                            report.getReported().getNickname(),
                            alert.getTitle(),
                            placeName,
                            alert.getCreatedAt(),
                            new GetReportResponse(
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

                    return new GetSignUpAlertResponse(
                            member.getId(),
                            member.getNickname(),
                            alert.getTitle(),
                            placeName,
                            member.getRecommender().getNickname(),
                            alert.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<GetTradeCompleteAlertResponse> createTradeCompleteAlertResponses(
            List<MemberDetail> memberDetails,
            List<AdminAlert> alerts,
            List<Product> products,
            List<ProductImage> productImages,
            Map<Long, String> memberIdToPlaceName
    ) {
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, List<GetImageResponse>> imageMap = productImages.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getProduct().getId(),
                        Collectors.mapping(ri -> new GetImageResponse(
                                ri.getImage().getId(),
                                ri.getImage().getImageUrl()), Collectors.toList())
                ));

        return alerts.stream()
                .map(alert -> {
                    MemberDetail memberDetail = memberDetails.stream()
                            .filter(md -> md.getMember().getId().equals(alert.getRequester().getId()))
                            .findFirst()
                            .orElseThrow(NotFoundMemberException::new);

                    String placeName = memberIdToPlaceName.get(alert.getRequester().getId());

                    Product product = Optional.ofNullable(productMap.get(alert.getSourceId()))
                            .orElseThrow(NotFoundProductException::new);

                    List<GetImageResponse> imageResponses = imageMap.getOrDefault(alert.getSourceId(), List.of());

                    int rawLight = memberDetail.getLight();
                    int light = Math.max(1, rawLight / 10);

                    GetProductResponse productResponse = new GetProductResponse(
                            product.getId(),
                            product.getTitle(),
                            product.getDescription(),
                            product.getGwangsan(),
                            product.getType(),
                            product.getMode(),
                            new GetProductMemberResponse(
                                    memberDetail.getId(),
                                    memberDetail.getMember().getNickname(),
                                    placeName,
                                    light
                            ),
                            imageResponses
                    );

                    return new GetTradeCompleteAlertResponse(
                            alert.getOtherMember().getNickname(),
                            alert.getTitle(),
                            alert.getCreatedAt(),
                            productResponse
                    );
                })
                .toList();
    }
}
