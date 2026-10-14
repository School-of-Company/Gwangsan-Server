package team.startup.gwangsan.adapter.integration.alert;

import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCancelSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCompleteSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReportSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReviewSource;
import team.startup.gwangsan.application.review.data.ReviewAuthor;
import team.startup.gwangsan.application.review.port.in.ReviewSummaryQuery;
import team.startup.gwangsan.application.trade.data.TradeCancelParties;
import team.startup.gwangsan.application.trade.data.TradeCompleteSummary;
import team.startup.gwangsan.application.trade.port.in.TradeSummaryQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.port.in.NoticeQuery;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.application.post.data.ProductImageLinkView;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.suspend.port.in.SuspensionQuery;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("알림 legacy bridge")
class AlertIntegrationAdapterTest {

    @Nested
    @DisplayName("AlertSourceAdapter 는")
    class Describe_source {

        private final MemberAccountQuery members = mock(MemberAccountQuery.class);
        private final TradeSummaryQuery trades = mock(TradeSummaryQuery.class);
        private final ProductSnapshotQuery products = mock(ProductSnapshotQuery.class);
        private final ReviewSummaryQuery reviews = mock(ReviewSummaryQuery.class);
        private final ReportQuery reports = mock(ReportQuery.class);
        private final SuspensionQuery suspends = mock(SuspensionQuery.class);
        private final TradeCancelQuery cancelQuery = mock(TradeCancelQuery.class);
        private final NoticeQuery notices = mock(NoticeQuery.class);
        private final AlertSourceAdapter adapter = new AlertSourceAdapter(
                members, trades, products, reviews, reports, suspends, cancelQuery, notices);

        @Test
        @DisplayName("원천이 없으면 기존 업무별 NotFound 예외를 그대로 던진다")
        void it_keeps_existing_not_found_exceptions() {
            when(members.findById(any())).thenReturn(Optional.empty());
            when(trades.getTradeComplete(anyLong())).thenThrow(new NotFoundTradeCompleteException());
            when(products.getActive(any())).thenThrow(new NotFoundProductException());
            when(reviews.getAuthor(anyLong())).thenThrow(new NotFoundReviewException());
            when(reports.getAlertSource(any())).thenThrow(new NotFoundReportException());
            when(suspends.getSuspendedDays(any())).thenThrow(new NotFoundSuspendException());
            when(cancelQuery.findRequesterId(anyLong())).thenReturn(Optional.empty());
            when(trades.getCancelParties(anyLong())).thenThrow(new NotFoundTradeCancelException());
            when(notices.getTitle(any())).thenThrow(new NoticeNotFoundException());

            assertThatThrownBy(() -> adapter.requireMember(1L)).isInstanceOf(NotFoundMemberException.class);
            assertThatThrownBy(() -> adapter.tradeComplete(1L)).isInstanceOf(NotFoundTradeCompleteException.class);
            assertThatThrownBy(() -> adapter.activeProductTitle(1L)).isInstanceOf(NotFoundProductException.class);
            assertThatThrownBy(() -> adapter.review(1L)).isInstanceOf(NotFoundReviewException.class);
            assertThatThrownBy(() -> adapter.report(1L)).isInstanceOf(NotFoundReportException.class);
            assertThatThrownBy(() -> adapter.suspendedDays(1L)).isInstanceOf(NotFoundSuspendException.class);
            assertThatThrownBy(() -> adapter.suspendedDays(null)).isInstanceOf(NotFoundSuspendException.class);
            assertThatThrownBy(() -> adapter.tradeCancelRequester(1L)).isInstanceOf(NotFoundTradeCancelException.class);
            assertThatThrownBy(() -> adapter.tradeCancelWithParties(1L)).isInstanceOf(NotFoundTradeCancelException.class);
            assertThatThrownBy(() -> adapter.noticeTitle(1L)).isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("거래 완료·철회 원천을 회원 ID 와 문구 값으로만 돌려준다")
        void it_maps_trade_sources() {
            when(trades.getTradeComplete(1L)).thenReturn(new TradeCompleteSummary(1L, 5L, "상품", 2L, "구매자", 3L));
            when(trades.getCancelParties(9L)).thenReturn(new TradeCancelParties(9L, 2L, 2L, 3L));

            assertThat(adapter.tradeComplete(1L)).isEqualTo(new TradeCompleteSource(5L, "상품", 2L, 3L, "구매자"));
            assertThat(adapter.tradeCancelWithParties(9L)).isEqualTo(new TradeCancelSource(2L, 2L, 3L));
            assertThat(adapter.recommenderReward()).isEqualTo(5000);
        }

        @Test
        @DisplayName("후기 원천은 review 공개 query 의 작성자 ID·별명을 그대로 옮긴다")
        void it_maps_review_source() {
            when(reviews.getAuthor(6L)).thenReturn(new ReviewAuthor(6L, 2L, "작성자"));

            assertThat(adapter.review(6L)).isEqualTo(new ReviewSource(2L, "작성자"));
        }

        @Test
        @DisplayName("공지 수신자 존재 확인과 철회 요청자는 member·trade 공개 query 값을 그대로 쓴다")
        void it_delegates_member_and_requester_to_public_queries() {
            when(members.findExistingIds(List.of(1L, 2L, 3L))).thenReturn(List.of(1L, 3L));
            when(cancelQuery.findRequesterId(9L)).thenReturn(Optional.of(2L));

            assertThat(adapter.existingMemberIds(List.of(1L, 2L, 3L))).containsExactly(1L, 3L);
            assertThat(adapter.tradeCancelRequester(9L)).isEqualTo(2L);
        }

        @Test
        @DisplayName("신고·정지 원천은 report/suspend 공개 query 값을 그대로 옮긴다")
        void it_maps_report_and_suspend_sources() {
            when(reports.getAlertSource(4L)).thenReturn(new ReportAlertSource(2L, "피신고자"));
            when(suspends.getSuspendedDays(7L)).thenReturn(3);

            assertThat(adapter.report(4L)).isEqualTo(new ReportSource(2L, "피신고자"));
            assertThat(adapter.suspendedDays(7L)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("AlertImageAdapter 는")
    class Describe_images {

        private final NoticeQuery noticeImages = mock(NoticeQuery.class);
        private final TradeCancelQuery tradeCancelQuery = mock(TradeCancelQuery.class);
        private final ProductImageQuery productImages = mock(ProductImageQuery.class);
        private final ReportQuery reportQuery = mock(ReportQuery.class);
        private final AlertImageAdapter adapter = new AlertImageAdapter(
                noticeImages, productImages, reportQuery, tradeCancelQuery);

        @Test
        @DisplayName("신고 이미지는 report 공개 query 의 연결 행 ID 를 imageId 로 쓴다")
        void it_maps_report_image_links() {
            when(reportQuery.findImageLinks(List.of(4L))).thenReturn(java.util.Map.of(4L, List.of(new ReportImageLink(31L, "https://r"))));

            assertThat(adapter.reportImages(List.of(4L))).isEqualTo(java.util.Map.of(4L, List.of(new AlertImage(31L, "https://r"))));
        }

        @Test
        @DisplayName("기존 응답처럼 imageId 에 연결 행 ID 를 넣는다")
        void it_uses_link_row_id_as_image_id() {
            when(noticeImages.findImageLinks(List.of(100L)))
                    .thenReturn(Map.of(100L, List.of(new NoticeImageLink(11L, 900L, "https://n"))));

            assertThat(adapter.noticeImages(List.of(100L))).isEqualTo(Map.of(100L, List.of(new AlertImage(11L, "https://n"))));
        }

        @Test
        @DisplayName("상품 이미지도 imageId 에 이미지 ID 가 아닌 연결 행 ID 를 넣는다")
        void it_uses_product_link_row_id() {
            when(productImages.findImageLinks(List.of(5L)))
                    .thenReturn(Map.of(5L, List.of(new ProductImageLinkView(31L, 900L, "https://p"))));

            assertThat(adapter.productImages(List.of(5L))).isEqualTo(Map.of(5L, List.of(new AlertImage(31L, "https://p"))));
        }

        @Test
        @DisplayName("거래 철회 → 상품 ID 는 trade 공개 TradeCancelQuery 결과를 그대로 쓴다(없는 철회는 빠짐)")
        void it_delegates_trade_cancel_products_to_trade_query() {
            when(tradeCancelQuery.findProductIds(List.of(1L, 2L))).thenReturn(Map.of(1L, 5L));

            assertThat(adapter.productIdsByTradeCancel(List.of(1L, 2L))).isEqualTo(Map.of(1L, 5L));
        }
    }
}
