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
import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.Product;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAlertByCurrentServiceImpl 단위 테스트")
class FindAlertByCurrentServiceImplTest {

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private AlertReceiptRepository alertReceiptRepository;

    @Mock
    private NoticeImageRepository noticeImageRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private TradeCancelRepository tradeCancelRepository;

    @Mock
    private ReportImageRepository reportImageRepository;

    @InjectMocks
    private FindAlertByCurrentServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("알림이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_alerts() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of());
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("REVIEW 타입 알림이 있으면 이미지 없이 응답을 반환한다")
        void it_returns_review_alert_with_empty_images() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(10L);
            when(alert.getAlertType()).thenReturn(AlertType.REVIEW);
            when(alert.getSourceId()).thenReturn(50L);
            when(alert.getTitle()).thenReturn("리뷰 등록");
            when(alert.getContent()).thenReturn("나에 대한 후기가 등록되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REVIEW);
            assertThat(result.get(0).images()).isEmpty();
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입 알림이 있으면 상품 이미지를 조회한다")
        void it_fetches_product_images_for_trade_complete_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(20L);
            when(alert.getAlertType()).thenReturn(AlertType.TRADE_COMPLETE);
            when(alert.getSourceId()).thenReturn(100L);
            when(alert.getTitle()).thenReturn("거래 완료");
            when(alert.getContent()).thenReturn("광산이 차감되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_COMPLETE);
            assertThat(result.get(0).images()).isEmpty();

            verify(productImageRepository).findAllByProductIdIn(anyList());
        }

        @Test
        @DisplayName("NOTICE 타입 알림이 있으면 공지 이미지를 조회한다")
        void it_fetches_notice_images_for_notice_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(40L);
            when(alert.getAlertType()).thenReturn(AlertType.NOTICE);
            when(alert.getSourceId()).thenReturn(200L);
            when(alert.getTitle()).thenReturn("공지사항");
            when(alert.getContent()).thenReturn("새로운 공지가 등록되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.NOTICE);
            assertThat(result.get(0).images()).isEmpty();

            verify(noticeImageRepository).findAllByNoticeIdIn(anyList());
        }

        @Test
        @DisplayName("REPORT 타입 알림이 있으면 신고 이미지를 조회한다")
        void it_fetches_report_images_for_report_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(50L);
            when(alert.getAlertType()).thenReturn(AlertType.REPORT);
            when(alert.getSourceId()).thenReturn(300L);
            when(alert.getTitle()).thenReturn("신고 접수");
            when(alert.getContent()).thenReturn("신고가 접수되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
            when(reportImageRepository.findAllByReportIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REPORT);
            assertThat(result.get(0).images()).isEmpty();

            verify(reportImageRepository).findAllByReportIdIn(anyList());
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입 알림이 있으면 거래 철회를 조회하고 상품 이미지를 조회한다")
        void it_fetches_product_images_via_trade_cancel_for_trade_cancel_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(60L);
            when(alert.getAlertType()).thenReturn(AlertType.TRADE_CANCEL);
            when(alert.getSourceId()).thenReturn(400L);
            when(alert.getTitle()).thenReturn("거래 철회 요청");
            when(alert.getContent()).thenReturn("거래 철회 요청이 접수되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(500L);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(400L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
            when(tradeCancelRepository.findAllById(anyList())).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_CANCEL);

            verify(tradeCancelRepository).findAllById(anyList());
            verify(productImageRepository).findAllByProductIdIn(anyList());
        }

        @Test
        @DisplayName("REPORT_REJECT 타입 알림이 있으면 신고 이미지를 조회한다")
        void it_fetches_report_images_for_report_reject_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(55L);
            when(alert.getAlertType()).thenReturn(AlertType.REPORT_REJECT);
            when(alert.getSourceId()).thenReturn(310L);
            when(alert.getTitle()).thenReturn("신고 기각");
            when(alert.getContent()).thenReturn("신고가 기각되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
            when(reportImageRepository.findAllByReportIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REPORT_REJECT);

            verify(reportImageRepository).findAllByReportIdIn(anyList());
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입 알림이 있으면 거래 철회를 조회하고 상품 이미지를 조회한다")
        void it_fetches_product_images_via_trade_cancel_for_trade_cancel_reject_alert() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(65L);
            when(alert.getAlertType()).thenReturn(AlertType.TRADE_CANCEL_REJECT);
            when(alert.getSourceId()).thenReturn(410L);
            when(alert.getTitle()).thenReturn("거래 철회 기각");
            when(alert.getContent()).thenReturn("거래 철회 요청이 기각되었습니다.");
            when(alert.getSendMember()).thenReturn(null);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(510L);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(410L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
            when(tradeCancelRepository.findAllById(anyList())).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_CANCEL_REJECT);

            verify(tradeCancelRepository).findAllById(anyList());
            verify(productImageRepository).findAllByProductIdIn(anyList());
        }

        @Test
        @DisplayName("sendMember 가 있는 알림이면 sendMemberId 를 포함해 반환한다")
        void it_includes_send_member_id_when_present() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            Member sendMember = mock(Member.class);
            when(sendMember.getId()).thenReturn(200L);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(30L);
            when(alert.getAlertType()).thenReturn(AlertType.RECOMMENDER);
            when(alert.getSourceId()).thenReturn(2L);
            when(alert.getTitle()).thenReturn("추천인 등록");
            when(alert.getContent()).thenReturn("추천인으로 등록되어 5000 광산이 지급되었습니다.");
            when(alert.getSendMember()).thenReturn(sendMember);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).sendMemberId()).isEqualTo(200L);
        }
    }
}
