package team.startup.gwangsan.adapter.web.admin.controller;

import team.startup.gwangsan.adapter.web.admin.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.adapter.web.admin.dto.response.GetReportAlertResponse;
import team.startup.gwangsan.adapter.web.admin.dto.response.GetSignUpAlertResponse;
import team.startup.gwangsan.adapter.web.admin.dto.response.GetTradeCancelAlertResponse;
import team.startup.gwangsan.adapter.web.admin.dto.response.SignInAdminResponse;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.admin.data.AdminImage;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminSignInResult;
import team.startup.gwangsan.adapter.web.auth.dto.response.TokenResponse;
import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.adapter.web.post.dto.GetProductMemberResponse;
import team.startup.gwangsan.adapter.web.post.dto.GetProductResponse;
import team.startup.gwangsan.adapter.web.post.dto.ProductImageResponse;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.adapter.web.report.dto.GetReportResponse;
import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.List;

/** application 결과 → 기존 HTTP 응답 DTO. JSON 이름·형태는 기존 응답과 같다. */
final class AdminWebMapper {

    private AdminWebMapper() {
    }

    static GetAdminAlertResponse toResponse(AdminAlertsResult result) {
        return new GetAdminAlertResponse(
                result.reports().stream().map(AdminWebMapper::toResponse).toList(),
                result.signUps().stream().map(AdminWebMapper::toResponse).toList(),
                result.tradeCancels().stream().map(AdminWebMapper::toResponse).toList()
        );
    }

    static SignInAdminResponse toResponse(AdminSignInResult result) {
        return new SignInAdminResponse(
                new TokenResponse(result.accessToken(), result.refreshToken(),
                        result.accessTokenExpiresIn(), result.refreshTokenExpiresIn()),
                MemberRole.valueOf(result.role().name())
        );
    }

    static GetProductResponse toResponse(AdminProduct product) {
        AdminProduct.Owner owner = product.member();
        return new GetProductResponse(
                product.id(),
                product.title(),
                product.content(),
                product.gwangsan(),
                product.type() == null ? null : Type.valueOf(product.type()),
                product.mode() == null ? null : Mode.valueOf(product.mode()),
                new GetProductMemberResponse(owner.memberId(), owner.nickname(), owner.placeName(), owner.light()),
                product.images().stream()
                        .map(image -> new ProductImageResponse(image.imageId(), image.imageUrl()))
                        .toList(),
                product.completed(),
                product.reserved()
        );
    }

    private static GetReportAlertResponse toResponse(AdminAlertsResult.ReportAlert alert) {
        return new GetReportAlertResponse(
                alert.id(),
                alert.nickname(),
                alert.reportedMemberId(),
                alert.reportedMemberName(),
                alert.productTarget() ? ReportTargetType.PRODUCT : ReportTargetType.MEMBER,
                alert.productId(),
                alert.productTitle(),
                alert.title(),
                alert.placeName(),
                alert.createdAt(),
                new GetReportResponse(
                        alert.reportId(),
                        alert.reportType() == null ? null : ReportType.valueOf(alert.reportType()),
                        alert.content(),
                        images(alert.images())
                )
        );
    }

    private static GetSignUpAlertResponse toResponse(AdminAlertsResult.SignUpAlert alert) {
        return new GetSignUpAlertResponse(alert.id(), alert.memberId(), alert.nickname(), alert.title(),
                alert.placeName(), alert.recommenderNickname(), alert.createdAt());
    }

    private static GetTradeCancelAlertResponse toResponse(AdminAlertsResult.TradeCancelAlert alert) {
        return new GetTradeCancelAlertResponse(alert.id(), alert.nickname(), alert.title(), alert.reason(),
                alert.placeName(), alert.createdAt(), images(alert.images()), toResponse(alert.product()));
    }

    private static List<GetImageResponse> images(List<AdminImage> images) {
        return images.stream().map(image -> new GetImageResponse(image.imageId(), image.imageUrl())).toList();
    }
}
