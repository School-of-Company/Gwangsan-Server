package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetReportAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetSignUpAlertResponse;
import team.startup.gwangsan.domain.admin.repository.custom.AdminAlertCustomRepository;
import team.startup.gwangsan.domain.admin.service.FindAlertByAlertTypeAndPlaceService;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.repository.custom.MemberCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.presentation.dto.response.GetReportResponse;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.report.repository.custom.ReportCustomRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAlertByAlertTypeAndPlaceServiceImpl implements FindAlertByAlertTypeAndPlaceService {

    private final MemberDetailRepository memberDetailRepository;
    private final ReportRepository reportRepository;
    private final AdminAlertCustomRepository adminAlertCustomRepository;
    private final MemberCustomRepository memberCustomRepository;
    private final MemberRepository memberRepository;
    private final ReportCustomRepository reportCustomRepository;
    private final ReportImageRepository reportImageRepository;
    private final PlaceRepository placeRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public GetAdminAlertResponse execute(String placeName, AlertType type) {
        Member admin = memberUtil.getCurrentMember();
        Place place = findTargetPlaceOrNull(placeName, admin);

        List<AdminAlert> alerts = adminAlertCustomRepository.findAdminAlertByPlaceAndAlertType(place, type);

        List<AdminAlert> reportAlerts = filterAlertsByType(alerts, AlertType.REPORT);
        List<AdminAlert> signUpAlerts = filterAlertsByType(alerts, AlertType.SIGN_UP);

        List<Report> reports;
        List<Member> members;

        if (place == null) {
            reports = reportRepository.findAll();
            members = memberRepository.findAllByStatus(MemberStatus.PENDING);
        } else {
            reports = reportCustomRepository.findByPlace(place);
            members = memberCustomRepository.findByStatusAndPlace(MemberStatus.PENDING, place);
        }

        List<ReportImage> reportImages = reportImageRepository.findByReportIn(reports);
        List<GetReportAlertResponse> reportAlertResponses = createReportAlertResponses(reportAlerts, reports, reportImages);

        List<GetSignUpAlertResponse> signUpAlertResponses = createSignUpAlertResponses(signUpAlerts, members);
        return new GetAdminAlertResponse(reportAlertResponses, signUpAlertResponses);
    }

    private List<AdminAlert> filterAlertsByType(List<AdminAlert> alerts, AlertType type) {
        return alerts.stream().filter(a -> a.getType().equals(type)).toList();
    }

    private Place findTargetPlaceOrNull(String placeName, Member member) {
        if (member.getRole() == MemberRole.ROLE_HEAD_ADMIN) {
            if (placeName == null) {
                return null;
            }
            return placeRepository.findByName(placeName)
                    .orElseThrow(PlaceNotFoundException::new);
        }

        return memberDetailRepository.findPlaceByMemberId(member.getId());
    }

    private List<GetReportAlertResponse> createReportAlertResponses(List<AdminAlert> alerts, List<Report> reports, List<ReportImage> images) {
        return alerts.stream()
                .map(alert -> {
                    Report report = reports.stream()
                            .filter(r -> r.getId().equals(alert.getSourceId()))
                            .findFirst()
                            .orElseThrow(NotFoundReportException::new);

                    List<GetImageResponse> imageResponses = images.stream()
                            .filter(ri -> ri.getReport().getId().equals(report.getId()))
                            .map(ri -> new GetImageResponse(
                                    ri.getImage().getId(),
                                    ri.getImage().getImageUrl()
                            ))
                            .toList();

                    return new GetReportAlertResponse(
                            report.getId(),
                            alert.getMember().getNickname(),
                            report.getReported().getId(),
                            report.getReported().getNickname(),
                            alert.getTitle(),
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

    private List<GetSignUpAlertResponse> createSignUpAlertResponses(List<AdminAlert> alerts, List<Member> members) {
        return alerts.stream()
                .map(alert -> {
                    Member member = members.stream()
                            .filter(m -> m.getId().equals(alert.getMember().getId()))
                            .findFirst()
                            .orElseThrow(NotFoundMemberException::new);

                    return new GetSignUpAlertResponse(
                            member.getId(),
                            member.getNickname(),
                            alert.getTitle(),
                            member.getRecommender().getNickname(),
                            alert.getCreatedAt()
                    );
                })
                .toList();
    }
}
