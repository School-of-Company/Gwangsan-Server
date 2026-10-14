package team.startup.gwangsan.adapter.integration.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCancelSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCompleteSource;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("알림 legacy bridge")
class LegacyAlertBridgeTest {

    @Nested
    @DisplayName("LegacyAlertSourceAdapter 는")
    class Describe_source {

        private final MemberAccountQuery members = mock(MemberAccountQuery.class);
        private final MemberJpaRepository memberJpaRepository = mock(MemberJpaRepository.class);
        private final TradeCompleteRepository trades = mock(TradeCompleteRepository.class);
        private final ProductRepository products = mock(ProductRepository.class);
        private final ReviewRepository reviews = mock(ReviewRepository.class);
        private final ReportRepository reports = mock(ReportRepository.class);
        private final SuspendRepository suspends = mock(SuspendRepository.class);
        private final TradeCancelRepository cancels = mock(TradeCancelRepository.class);
        private final NoticeRepository notices = mock(NoticeRepository.class);
        private final LegacyAlertSourceAdapter adapter = new LegacyAlertSourceAdapter(
                members, memberJpaRepository, trades, products, reviews, reports, suspends, cancels, notices);

        @Test
        @DisplayName("원천이 없으면 기존 업무별 NotFound 예외를 그대로 던진다")
        void it_keeps_existing_not_found_exceptions() {
            when(members.findById(any())).thenReturn(Optional.empty());
            when(trades.findById(any())).thenReturn(Optional.empty());
            when(products.findActiveById(any())).thenReturn(Optional.empty());
            when(reviews.findById(any())).thenReturn(Optional.empty());
            when(reports.findById(any())).thenReturn(Optional.empty());
            when(suspends.findById(any())).thenReturn(Optional.empty());
            when(cancels.findById(any())).thenReturn(Optional.empty());
            when(cancels.findByIdWithTradeCompleteAndMember(any())).thenReturn(Optional.empty());
            when(notices.findById(any())).thenReturn(Optional.empty());

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
            MemberJpaEntity buyer = member(2L, "구매자");
            MemberJpaEntity seller = member(3L, "판매자");
            Product product = mock(Product.class);
            when(product.getId()).thenReturn(5L);
            when(product.getTitle()).thenReturn("상품");
            TradeComplete trade = mock(TradeComplete.class);
            when(trade.getProduct()).thenReturn(product);
            when(trade.getBuyer()).thenReturn(buyer);
            when(trade.getSeller()).thenReturn(seller);
            when(trades.findById(1L)).thenReturn(Optional.of(trade));
            TradeCancel cancel = mock(TradeCancel.class);
            when(cancel.getMember()).thenReturn(buyer);
            when(cancel.getTradeComplete()).thenReturn(trade);
            when(cancels.findByIdWithTradeCompleteAndMember(9L)).thenReturn(Optional.of(cancel));

            assertThat(adapter.tradeComplete(1L)).isEqualTo(new TradeCompleteSource(5L, "상품", 2L, 3L, "구매자"));
            assertThat(adapter.tradeCancelWithParties(9L)).isEqualTo(new TradeCancelSource(2L, 2L, 3L));
            assertThat(adapter.recommenderReward()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("LegacyAlertImageAdapter 는")
    class Describe_images {

        private final NoticeImageRepository noticeImages = mock(NoticeImageRepository.class);
        private final TradeCancelRepository cancels = mock(TradeCancelRepository.class);
        private final LegacyAlertImageAdapter adapter = new LegacyAlertImageAdapter(
                noticeImages, mock(ProductImageRepository.class), mock(ReportImageRepository.class), cancels);

        @Test
        @DisplayName("기존 응답처럼 imageId 에 연결 행 ID 를 넣는다")
        void it_uses_link_row_id_as_image_id() {
            Notice notice = mock(Notice.class);
            when(notice.getId()).thenReturn(100L);
            NoticeImage link = mock(NoticeImage.class);
            when(link.getId()).thenReturn(11L);
            when(link.getNotice()).thenReturn(notice);
            when(link.getImage()).thenReturn(Image.builder().imageUrl("https://n").build());
            when(noticeImages.findAllByNoticeIdIn(List.of(100L))).thenReturn(List.of(link));

            assertThat(adapter.noticeImages(List.of(100L))).isEqualTo(Map.of(100L, List.of(new AlertImage(11L, "https://n"))));
        }

        @Test
        @DisplayName("상품이 없는 거래 철회는 결과에서 뺀다")
        void it_skips_trade_cancel_without_product() {
            TradeCancel orphan = mock(TradeCancel.class);
            when(cancels.findAllById(List.of(1L))).thenReturn(List.of(orphan));

            assertThat(adapter.productIdsByTradeCancel(List.of(1L))).isEmpty();
        }
    }

    private static MemberJpaEntity member(Long id, String nickname) {
        MemberJpaEntity member = mock(MemberJpaEntity.class);
        when(member.getId()).thenReturn(id);
        when(member.getNickname()).thenReturn(nickname);
        return member;
    }
}
