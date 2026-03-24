package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.exception.NotFoundAlertTypeException;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

        private Member member;

        @BeforeEach
        void setUp() {
            member = mock(Member.class);
            lenient().when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 NotFoundMemberException 을 던진다")
        void it_throws_NotFoundMemberException_when_member_not_found() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundMemberException.class,
                    () -> service.execute(1L, 99L, AlertType.REVIEW, null));
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입이면 buyer 에게 차감, seller 에게 추가 content 로 Alert 를 저장한다")
        void it_saves_two_receipts_for_trade_complete() {
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

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(100L, 1L, AlertType.TRADE_COMPLETE, null);

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository, times(2)).save(alertCaptor.capture());
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository, times(2)).save(receiptCaptor.capture());

            List<Alert> savedAlerts = alertCaptor.getAllValues();
            assertThat(savedAlerts.get(0).getSourceId()).isEqualTo(10L);
            assertThat(savedAlerts.get(0).getAlertType()).isEqualTo(AlertType.TRADE_COMPLETE);
            assertThat(savedAlerts.get(0).getSendMember()).isSameAs(seller);
            assertThat(savedAlerts.get(0).getContent()).isEqualTo("광산이 차감되었습니다.");
            assertThat(savedAlerts.get(1).getSourceId()).isEqualTo(10L);
            assertThat(savedAlerts.get(1).getSendMember()).isSameAs(seller);
            assertThat(savedAlerts.get(1).getContent()).isEqualTo("광산이 추가되었습니다.");

            List<AlertReceipt> receipts = receiptCaptor.getAllValues();
            assertThat(receipts.get(0).getMember()).isSameAs(buyer);
            assertThat(receipts.get(1).getMember()).isSameAs(seller);
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입인데 거래 완료 내역이 없으면 NotFoundTradeCompleteException 을 던진다")
        void it_throws_NotFoundTradeCompleteException_for_trade_complete() {
            when(tradeCompleteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCompleteException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_COMPLETE, null));
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 타입이면 product 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_trade_complete_reject() {
            Product product = mock(Product.class);
            when(product.getTitle()).thenReturn("상품명");
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(100L, 1L, AlertType.TRADE_COMPLETE_REJECT, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.TRADE_COMPLETE_REJECT);
            assertThat(captor.getValue().getTitle()).isEqualTo("상품명");
            assertThat(captor.getValue().getContent()).isEqualTo("거래 완료가 거절되었습니다.");
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 타입인데 상품이 없으면 NotFoundProductException 을 던진다")
        void it_throws_NotFoundProductException_for_trade_complete_reject() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundProductException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_COMPLETE_REJECT, null));
        }

        @Test
        @DisplayName("OTHER_MEMBER_TRADE_COMPLETE 타입이면 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_other_member_trade_complete() {
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

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(100L, 1L, AlertType.OTHER_MEMBER_TRADE_COMPLETE, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getSourceId()).isEqualTo(10L);
            assertThat(captor.getValue().getContent()).isEqualTo("구매자님이 거래를 완료하였습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(seller);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("REVIEW 타입이면 리뷰 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_review() {
            Member reviewer = mock(Member.class);
            when(reviewer.getNickname()).thenReturn("리뷰어");
            Review review = mock(Review.class);
            when(review.getReviewer()).thenReturn(reviewer);
            when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(50L, 1L, AlertType.REVIEW, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("리뷰어리뷰 등록");
            assertThat(captor.getValue().getContent()).isEqualTo("나에 대한 후기가 등록되었습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(reviewer);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("REVIEW 타입인데 리뷰가 없으면 NotFoundReviewException 을 던진다")
        void it_throws_NotFoundReviewException_for_review() {
            when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundReviewException.class,
                    () -> service.execute(99L, 1L, AlertType.REVIEW, null));
        }

        @Test
        @DisplayName("REPORT_REJECT 타입이면 신고 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_report_reject() {
            Member reporter = mock(Member.class);
            Member reported = mock(Member.class);
            when(reported.getNickname()).thenReturn("신고대상");
            Report report = mock(Report.class);
            when(report.getReporter()).thenReturn(reporter);
            when(report.getReported()).thenReturn(reported);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(30L, 1L, AlertType.REPORT_REJECT, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("신고 기각");
            assertThat(captor.getValue().getContent()).isEqualTo("신고대상님에 대한 신고가 기각되었습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(reporter);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("REPORT_REJECT 타입인데 신고가 없으면 NotFoundReportException 을 던진다")
        void it_throws_NotFoundReportException_for_report_reject() {
            when(reportRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundReportException.class,
                    () -> service.execute(99L, 1L, AlertType.REPORT_REJECT, null));
        }

        @Test
        @DisplayName("REPORT 타입이면 신고 및 정지 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_report() {
            Member reporter = mock(Member.class);
            Report report = mock(Report.class);
            when(report.getReporter()).thenReturn(reporter);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            Suspend suspend = mock(Suspend.class);
            when(suspend.getSuspendedDays()).thenReturn(7);
            when(suspendRepository.findById(5L)).thenReturn(Optional.of(suspend));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(30L, 1L, AlertType.REPORT, 5L);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("신고 결과");
            assertThat(captor.getValue().getContent()).isEqualTo("7일 정지 조치되었습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(reporter);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("REPORT 타입인데 suspendId 가 null 이면 NotFoundSuspendException 을 던진다")
        void it_throws_NotFoundSuspendException_when_suspend_id_is_null() {
            Report report = mock(Report.class);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));
            when(suspendRepository.findById(null)).thenReturn(Optional.empty());

            assertThrows(NotFoundSuspendException.class,
                    () -> service.execute(30L, 1L, AlertType.REPORT, null));
        }

        @Test
        @DisplayName("REPORT 타입인데 정지 내역이 없으면 NotFoundSuspendException 을 던진다")
        void it_throws_NotFoundSuspendException_when_suspend_not_found() {
            Report report = mock(Report.class);
            when(reportRepository.findById(30L)).thenReturn(Optional.of(report));

            when(suspendRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundSuspendException.class,
                    () -> service.execute(30L, 1L, AlertType.REPORT, 99L));
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입이면 buyer/seller 각각 AlertReceipt 를 저장한다")
        void it_saves_receipts_for_trade_cancel() {
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

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(20L, 1L, AlertType.TRADE_CANCEL, null);

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(alertCaptor.capture());
            assertThat(alertCaptor.getValue().getTitle()).isEqualTo("거래 철회 완료");
            assertThat(alertCaptor.getValue().getContent()).isEqualTo("거래가 철회되었습니다.");
            assertThat(alertCaptor.getValue().getSendMember()).isSameAs(cancelMember);

            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository, times(2)).save(receiptCaptor.capture());
            List<AlertReceipt> receipts = receiptCaptor.getAllValues();
            assertThat(receipts.get(0).getMember()).isSameAs(buyer);
            assertThat(receipts.get(1).getMember()).isSameAs(seller);
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입이면 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_trade_cancel_reject() {
            Member cancelMember = mock(Member.class);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getMember()).thenReturn(cancelMember);
            when(tradeCancelRepository.findById(20L)).thenReturn(Optional.of(tradeCancel));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(20L, 1L, AlertType.TRADE_CANCEL_REJECT, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("거래 철회 기각");
            assertThat(captor.getValue().getContent()).isEqualTo("거래 철회 요청이 기각되었습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(cancelMember);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입인데 거래 철회가 없으면 NotFoundTradeCancelException 을 던진다")
        void it_throws_NotFoundTradeCancelException_for_trade_cancel_reject() {
            when(tradeCancelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCancelException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_CANCEL_REJECT, null));
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입인데 거래 철회가 없으면 NotFoundTradeCancelException 을 던진다")
        void it_throws_NotFoundTradeCancelException_for_trade_cancel() {
            when(tradeCancelRepository.findByIdWithTradeCompleteAndMember(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCancelException.class,
                    () -> service.execute(99L, 1L, AlertType.TRADE_CANCEL, null));
        }

        @Test
        @DisplayName("RECOMMENDER 타입이면 추천인 조회 후 member 에게 AlertReceipt 를 저장한다")
        void it_saves_receipt_for_recommender() {
            Member signUpMember = mock(Member.class);
            when(memberRepository.findById(50L)).thenReturn(Optional.of(signUpMember));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(50L, 1L, AlertType.RECOMMENDER, null);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("추천인 등록");
            assertThat(captor.getValue().getContent()).isEqualTo("추천인으로 등록되어 5000 광산이 지급되었습니다.");
            assertThat(captor.getValue().getSendMember()).isSameAs(signUpMember);
            ArgumentCaptor<AlertReceipt> receiptCaptor = ArgumentCaptor.forClass(AlertReceipt.class);
            verify(alertReceiptRepository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getMember()).isSameAs(member);
        }

        @Test
        @DisplayName("RECOMMENDER 타입인데 추천인이 없으면 NotFoundMemberException 을 던진다")
        void it_throws_NotFoundMemberException_when_recommender_not_found() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundMemberException.class,
                    () -> service.execute(99L, 1L, AlertType.RECOMMENDER, null));
        }

        @Test
        @DisplayName("NOTICE 타입이면 NotFoundAlertTypeException 을 던진다")
        void it_throws_NotFoundAlertTypeException_for_notice_type() {
            assertThrows(NotFoundAlertTypeException.class,
                    () -> service.execute(1L, 1L, AlertType.NOTICE, null));
        }
    }
}
