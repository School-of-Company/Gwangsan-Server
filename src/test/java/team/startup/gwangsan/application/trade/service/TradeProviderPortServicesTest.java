package team.startup.gwangsan.application.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.review.port.out.ReviewPort;
import team.startup.gwangsan.application.review.service.ReviewMemberReferenceCleanupService;
import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.application.trade.port.out.TradeMemberReferencePort;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정리 라운드 A trade/review provider 공개 Port 단위 테스트")
class TradeProviderPortServicesTest {

    @Mock private TradeCancelPort tradeCancelPort;
    @Mock private TradeMemberReferencePort tradeMemberReferencePort;
    @Mock private ReviewPort reviewPort;

    @Nested
    @DisplayName("RejectTradeCancelService 는")
    class Describe_reject {

        private RejectTradeCancelService service() {
            return new RejectTradeCancelService(tradeCancelPort);
        }

        @Test
        @DisplayName("PENDING 철회를 REJECTED 로 저장하고 요청 회원 ID 를 돌려준다")
        void it_rejects_pending() {
            when(tradeCancelPort.findBasicById(20L)).thenReturn(Optional.of(new TradeCancel(20L, 10L, 2L, "사유", TradeCancelStatus.PENDING)));

            assertThat(service().reject(20L)).isEqualTo(2L);

            ArgumentCaptor<TradeCancel> saved = ArgumentCaptor.forClass(TradeCancel.class);
            verify(tradeCancelPort).update(saved.capture());
            assertThat(saved.getValue().status()).isEqualTo(TradeCancelStatus.REJECTED);
        }

        @Test
        @DisplayName("이미 처리된 철회면 CannotPendingTradeCancelException 을 던지고 저장하지 않는다")
        void it_rejects_only_pending() {
            when(tradeCancelPort.findBasicById(20L)).thenReturn(Optional.of(new TradeCancel(20L, 10L, 2L, "사유", TradeCancelStatus.APPROVED)));

            assertThatThrownBy(() -> service().reject(20L)).isInstanceOf(CannotPendingTradeCancelException.class);
            verify(tradeCancelPort, never()).update(any());
        }

        @Test
        @DisplayName("없으면 NotFoundTradeCancelException 을 던진다")
        void it_requires_cancel() {
            when(tradeCancelPort.findBasicById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().reject(20L)).isInstanceOf(NotFoundTradeCancelException.class);
        }
    }

    @Test
    @DisplayName("거래 회원 정리는 구매자 → 판매자 순서로, 철회 요청자는 따로 재지정한다")
    void it_reassigns_trade_references_in_legacy_order() {
        TradeMemberReferenceCleanupService service = new TradeMemberReferenceCleanupService(tradeMemberReferencePort);

        service.reassignTradeParties(1L, 99L);
        service.reassignCancelRequester(1L, 99L);

        InOrder order = inOrder(tradeMemberReferencePort);
        order.verify(tradeMemberReferencePort).reassignBuyer(1L, 99L);
        order.verify(tradeMemberReferencePort).reassignSeller(1L, 99L);
        order.verify(tradeMemberReferencePort).reassignCancelRequester(1L, 99L);
    }

    @Test
    @DisplayName("후기 회원 정리는 작성자 → 대상 순서로 재지정한다")
    void it_reassigns_review_references_in_legacy_order() {
        new ReviewMemberReferenceCleanupService(reviewPort).reassign(1L, 99L);

        InOrder order = inOrder(reviewPort);
        order.verify(reviewPort).reassignReviewer(1L, 99L);
        order.verify(reviewPort).reassignReviewed(1L, 99L);
    }

    @Nested
    @DisplayName("TradeCancelQueryService 는")
    class Describe_query {

        private TradeCancelQueryService service() {
            return new TradeCancelQueryService(tradeCancelPort);
        }

        @Test
        @DisplayName("빈 입력이면 저장소를 조회하지 않는다(기존 소비자의 빈 목록 분기와 같음)")
        void it_skips_empty_input() {
            assertThat(service().findAll(List.of())).isEmpty();
            assertThat(service().findImages(List.of())).isEmpty();
            verifyNoInteractions(tradeCancelPort);
        }

        @Test
        @DisplayName("목록·이미지·요청자 조회를 저장소 결과 그대로 돌려준다")
        void it_delegates() {
            TradeCancelView view = new TradeCancelView(20L, 100L, 2L, "사유", TradeCancelStatus.PENDING);
            when(tradeCancelPort.findViews(List.of(20L))).thenReturn(List.of(view));
            when(tradeCancelPort.findImageViews(List.of(20L))).thenReturn(Map.of(20L, List.of(new TradeCancelImageView(5L, "u"))));
            when(tradeCancelPort.findRequesterId(20L)).thenReturn(Optional.of(2L));

            assertThat(service().findAll(List.of(20L))).containsExactly(view);
            assertThat(service().findImages(List.of(20L))).containsEntry(20L, List.of(new TradeCancelImageView(5L, "u")));
            assertThat(service().findRequesterId(20L)).contains(2L);
        }
    }
}
