package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAlertServiceImpl 단위 테스트")
class CreateAlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private TradeCompleteRepository tradeCompleteRepository;
    @Mock
    private AlertReceiptRepository alertReceiptRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private TradeCancelRepository tradeCancelRepository;
    @Mock
    private SuspendRepository suspendRepository;

    @InjectMocks
    private CreateAlertServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("회원이 존재하지 않으면 NotFoundMemberException 을 던진다")
        void it_throws_NotFoundMemberException_when_member_not_found() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundMemberException.class,
                    () -> service.execute(1L, 99L, AlertType.REVIEW, null));
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입이면 buyer/seller 각각 AlertReceipt 를 저장한다")
        void it_saves_two_receipts_for_trade_complete() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member buyer = mock(Member.class);
            Member seller = mock(Member.class);
            Product product = mock(Product.class);
            when(product.getId()).thenReturn(10L);
            when(product.getTitle()).thenReturn("상품명");

            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            when(tradeComplete.getBuyer()).thenReturn(buyer);
            when(tradeComplete.getSeller()).thenReturn(seller);
            when(tradeCompleteRepository.findById(100L)).thenReturn(Optional.of(tradeComplete));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(100L, 1L, AlertType.TRADE_COMPLETE, null);

            verify(alertRepository, times(2)).save(any());
            verify(alertReceiptRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입인데 거래 완료 내역이 없으면 NotFoundTradeCompleteException 을 던진다")
        void it_throws_NotFoundTradeCompleteException_for_trade_complete() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(tradeCompleteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCompleteException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_COMPLETE, null));
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 타입이면 product 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_trade_complete_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Product product = mock(Product.class);
            when(product.getTitle()).thenReturn("상품명");
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(100L, 1L, AlertType.TRADE_COMPLETE_REJECT, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 타입인데 상품이 없으면 NotFoundProductException 을 던진다")
        void it_throws_NotFoundProductException_for_trade_complete_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundProductException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_COMPLETE_REJECT, null));
        }

        @Test
        @DisplayName("OTHER_MEMBER_TRADE_COMPLETE 타입이면 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_other_member_trade_complete() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member buyer = mock(Member.class);
            when(buyer.getNickname()).thenReturn("구매자");
            Member seller = mock(Member.class);
            Product product = mock(Product.class);
            when(product.getId()).thenReturn(10L);
            when(product.getTitle()).thenReturn("상품명");

            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            when(tradeComplete.getBuyer()).thenReturn(buyer);
            when(tradeComplete.getSeller()).thenReturn(seller);
            when(tradeCompleteRepository.findById(100L)).thenReturn(Optional.of(tradeComplete));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(100L, 1L, AlertType.OTHER_MEMBER_TRADE_COMPLETE, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("REVIEW 타입이면 리뷰 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_review() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member reviewer = mock(Member.class);
            when(reviewer.getNickname()).thenReturn("리뷰어");
            Review review = mock(Review.class);
            when(review.getReviewer()).thenReturn(reviewer);
            when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(50L, 1L, AlertType.REVIEW, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("REVIEW 타입인데 리뷰가 없으면 NotFoundReviewException 을 던진다")
        void it_throws_NotFoundReviewException_for_review() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundReviewException.class,
                    () -> service.execute(99L, 1L, AlertType.REVIEW, null));
        }

        @Test
        @DisplayName("REPORT_REJECT 타입이면 신고 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_report_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member reporter = mock(Member.class);
            Member reported = mock(Member.class);
            when(reported.getNickname()).thenReturn("신고대상");
            Report report = mock(Report.class);
            when(report.getReporter()).thenReturn(reporter);
            when(report.getReported()).thenReturn(reported);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(30L, 1L, AlertType.REPORT_REJECT, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("REPORT_REJECT 타입인데 신고가 없으면 NotFoundReportException 을 던진다")
        void it_throws_NotFoundReportException_for_report_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(reportRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundReportException.class,
                    () -> service.execute(99L, 1L, AlertType.REPORT_REJECT, null));
        }

        @Test
        @DisplayName("REPORT 타입이면 신고 및 정지 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_report() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member reporter = mock(Member.class);
            Report report = mock(Report.class);
            when(report.getReporter()).thenReturn(reporter);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            Suspend suspend = mock(Suspend.class);
            when(suspend.getSuspendedDays()).thenReturn(7);
            when(suspendRepository.findById(5L)).thenReturn(Optional.of(suspend));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(30L, 1L, AlertType.REPORT, 5L);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("REPORT 타입인데 정지 내역이 없으면 NotFoundSuspendException 을 던진다")
        void it_throws_NotFoundSuspendException_when_suspend_not_found() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Report report = mock(Report.class);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            when(suspendRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundSuspendException.class,
                    () -> service.execute(30L, 1L, AlertType.REPORT, 99L));
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입이면 buyer/seller 각각 AlertReceipt 를 저장한다")
        void it_saves_receipts_for_trade_cancel() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member buyer = mock(Member.class);
            Member seller = mock(Member.class);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getBuyer()).thenReturn(buyer);
            when(tradeComplete.getSeller()).thenReturn(seller);

            Member cancelMember = mock(Member.class);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getMember()).thenReturn(cancelMember);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);
            when(tradeCancelRepository.findByIdWithTradeCompleteAndMember(20L)).thenReturn(Optional.of(tradeCancel));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(20L, 1L, AlertType.TRADE_CANCEL, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입이면 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_trade_cancel_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            Member cancelMember = mock(Member.class);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getMember()).thenReturn(cancelMember);
            when(tradeCancelRepository.findById(20L)).thenReturn(Optional.of(tradeCancel));

            Alert savedAlert = mock(Alert.class);
            when(alertRepository.save(any())).thenReturn(savedAlert);

            service.execute(20L, 1L, AlertType.TRADE_CANCEL_REJECT, null);

            verify(alertRepository).save(any());
            verify(alertReceiptRepository).save(any());
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입인데 거래 철회가 없으면 NotFoundTradeCancelException 을 던진다")
        void it_throws_NotFoundTradeCancelException_for_trade_cancel_reject() {
            Member member = mock(Member.class);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(tradeCancelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCancelException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_CANCEL_REJECT, null));
        }
    }
}
