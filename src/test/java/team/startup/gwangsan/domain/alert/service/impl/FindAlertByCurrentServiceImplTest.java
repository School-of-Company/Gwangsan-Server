package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.BeforeEach;
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
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.image.entity.Image;

import java.time.LocalDateTime;
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

    private static Alert mockAlert(long id, AlertType type, long sourceId, String title, String content) {
        Alert alert = mock(Alert.class);
        when(alert.getId()).thenReturn(id);
        when(alert.getAlertType()).thenReturn(type);
        when(alert.getSourceId()).thenReturn(sourceId);
        when(alert.getTitle()).thenReturn(title);
        when(alert.getContent()).thenReturn(content);
        when(alert.getSendMember()).thenReturn(null);
        when(alert.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        return alert;
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member member;

        @BeforeEach
        void setUp() {
            member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            lenient().when(noticeImageRepository.findAllByNoticeIdIn(anyList())).thenReturn(List.of());
        }

        @Test
        @DisplayName("알림이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_alerts() {
            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("REVIEW 타입 알림이 있으면 이미지 없이 응답을 반환하고 createdAt, sourceId 가 매핑된다")
        void it_returns_review_alert_with_empty_images() {
            LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 12, 0);
            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(10L);
            when(alert.getAlertType()).thenReturn(AlertType.REVIEW);
            when(alert.getSourceId()).thenReturn(50L);
            when(alert.getTitle()).thenReturn("리뷰 등록");
            when(alert.getContent()).thenReturn("나에 대한 후기가 등록되었습니다.");
            when(alert.getSendMember()).thenReturn(null);
            when(alert.getCreatedAt()).thenReturn(createdAt);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REVIEW);
            assertThat(result.get(0).createdAt()).isEqualTo(createdAt);
            assertThat(result.get(0).sourceId()).isEqualTo(50L);
            assertThat(result.get(0).images()).isEmpty();
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입 알림이 있으면 상품 이미지를 조회한다")
        void it_fetches_product_images_for_trade_complete_alert() {
            Alert alert = mockAlert(20L, AlertType.TRADE_COMPLETE, 100L, "거래 완료", "광산이 차감되었습니다.");

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(productImageRepository.findAllByProductIdIn(List.of(100L))).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_COMPLETE);
            assertThat(result.get(0).images()).isEmpty();

            verify(productImageRepository).findAllByProductIdIn(List.of(100L));
        }

        @Test
        @DisplayName("NOTICE 타입 알림이 있으면 공지 이미지를 조회한다")
        void it_fetches_notice_images_for_notice_alert() {
            Alert alert = mockAlert(40L, AlertType.NOTICE, 200L, "공지사항", "새로운 공지가 등록되었습니다.");

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.NOTICE);
            assertThat(result.get(0).images()).isEmpty();

            verify(noticeImageRepository).findAllByNoticeIdIn(List.of(200L));
        }

        @Test
        @DisplayName("REPORT 타입 알림이 있으면 신고 이미지를 조회한다")
        void it_fetches_report_images_for_report_alert() {
            Alert alert = mockAlert(50L, AlertType.REPORT, 300L, "신고 접수", "신고가 접수되었습니다.");

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(reportImageRepository.findAllByReportIdIn(List.of(300L))).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REPORT);
            assertThat(result.get(0).images()).isEmpty();

            verify(reportImageRepository).findAllByReportIdIn(List.of(300L));
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입 알림이 있으면 거래 철회를 조회하고 상품 이미지를 조회한다")
        void it_fetches_product_images_via_trade_cancel_for_trade_cancel_alert() {
            Alert alert = mockAlert(60L, AlertType.TRADE_CANCEL, 400L, "거래 철회 요청", "거래 철회 요청이 접수되었습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(500L);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(400L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(tradeCancelRepository.findAllById(List.of(400L))).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(List.of(500L))).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_CANCEL);

            verify(tradeCancelRepository).findAllById(List.of(400L));
            verify(productImageRepository).findAllByProductIdIn(List.of(500L));
        }

        @Test
        @DisplayName("REPORT_REJECT 타입 알림이 있으면 신고 이미지를 조회한다")
        void it_fetches_report_images_for_report_reject_alert() {
            Alert alert = mockAlert(55L, AlertType.REPORT_REJECT, 310L, "신고 기각", "신고가 기각되었습니다.");

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(reportImageRepository.findAllByReportIdIn(List.of(310L))).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.REPORT_REJECT);

            verify(reportImageRepository).findAllByReportIdIn(List.of(310L));
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 타입 알림이 있으면 거래 철회를 조회하고 상품 이미지를 조회한다")
        void it_fetches_product_images_via_trade_cancel_for_trade_cancel_reject_alert() {
            Alert alert = mockAlert(65L, AlertType.TRADE_CANCEL_REJECT, 410L, "거래 철회 기각", "거래 철회 요청이 기각되었습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(510L);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(410L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(tradeCancelRepository.findAllById(List.of(410L))).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(List.of(510L))).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_CANCEL_REJECT);

            verify(tradeCancelRepository).findAllById(List.of(410L));
            verify(productImageRepository).findAllByProductIdIn(List.of(510L));
        }

        @Test
        @DisplayName("sendMember 가 있는 알림이면 sendMemberId 를 포함해 반환한다")
        void it_includes_send_member_id_when_present() {
            Member sendMember = mock(Member.class);
            when(sendMember.getId()).thenReturn(200L);

            Alert alert = mock(Alert.class);
            when(alert.getId()).thenReturn(30L);
            when(alert.getAlertType()).thenReturn(AlertType.RECOMMENDER);
            when(alert.getSourceId()).thenReturn(2L);
            when(alert.getTitle()).thenReturn("추천인 등록");
            when(alert.getContent()).thenReturn("추천인으로 등록되어 5000 광산이 지급되었습니다.");
            when(alert.getSendMember()).thenReturn(sendMember);
            when(alert.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).sendMemberId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 타입 알림에 상품 이미지가 있으면 이미지 URL 을 반환한다")
        void it_returns_product_image_url_for_trade_complete_reject_alert() {
            Alert alert = mockAlert(21L, AlertType.TRADE_COMPLETE_REJECT, 110L, "거래 완료 거절", "거래 완료가 거절되었습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(110L);
            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/reject");
            ProductImage productImage = mock(ProductImage.class);
            when(productImage.getId()).thenReturn(6L);
            when(productImage.getProduct()).thenReturn(product);
            when(productImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(productImageRepository.findAllByProductIdIn(List.of(110L))).thenReturn(List.of(productImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.TRADE_COMPLETE_REJECT);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/reject");
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입 알림에 상품 이미지가 있으면 이미지 URL 을 반환한다")
        void it_returns_product_image_url_for_trade_complete_alert() {
            Alert alert = mockAlert(20L, AlertType.TRADE_COMPLETE, 100L, "거래 완료", "광산이 차감되었습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(100L);
            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/product");
            ProductImage productImage = mock(ProductImage.class);
            when(productImage.getId()).thenReturn(1L);
            when(productImage.getProduct()).thenReturn(product);
            when(productImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(productImageRepository.findAllByProductIdIn(List.of(100L))).thenReturn(List.of(productImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/product");
            verify(productImageRepository).findAllByProductIdIn(List.of(100L));
        }

        @Test
        @DisplayName("NOTICE 타입 알림에 공지 이미지가 있으면 이미지 URL 을 반환한다")
        void it_returns_notice_image_url_for_notice_alert() {
            Alert alert = mockAlert(40L, AlertType.NOTICE, 200L, "공지사항", "새로운 공지가 등록되었습니다.");

            Notice notice = mock(Notice.class);
            when(notice.getId()).thenReturn(200L);
            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/notice");
            NoticeImage noticeImage = mock(NoticeImage.class);
            when(noticeImage.getId()).thenReturn(2L);
            when(noticeImage.getNotice()).thenReturn(notice);
            when(noticeImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(noticeImageRepository.findAllByNoticeIdIn(List.of(200L))).thenReturn(List.of(noticeImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/notice");
            verify(noticeImageRepository).findAllByNoticeIdIn(List.of(200L));
        }

        @Test
        @DisplayName("REPORT 타입 알림에 신고 이미지가 있으면 이미지 URL 을 반환한다")
        void it_returns_report_image_url_for_report_alert() {
            Alert alert = mockAlert(50L, AlertType.REPORT, 300L, "신고 결과", "7일 정지 조치되었습니다.");

            team.startup.gwangsan.domain.report.entity.Report report =
                    mock(team.startup.gwangsan.domain.report.entity.Report.class);
            when(report.getId()).thenReturn(300L);
            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/report");
            ReportImage reportImage = mock(ReportImage.class);
            when(reportImage.getId()).thenReturn(3L);
            when(reportImage.getReport()).thenReturn(report);
            when(reportImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(reportImageRepository.findAllByReportIdIn(List.of(300L))).thenReturn(List.of(reportImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/report");
            verify(reportImageRepository).findAllByReportIdIn(List.of(300L));
        }

        @Test
        @DisplayName("TRADE_CANCEL 타입 알림에 상품 이미지가 있으면 tradeCancelId → productId 경유로 이미지 URL 을 반환한다")
        void it_returns_product_image_url_via_trade_cancel_mapping() {
            Alert alert = mockAlert(60L, AlertType.TRADE_CANCEL, 400L, "거래 철회 완료", "거래가 철회되었습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(500L);
            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(product);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(400L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/trade-cancel");
            ProductImage productImage = mock(ProductImage.class);
            when(productImage.getProduct()).thenReturn(product);
            when(productImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(tradeCancelRepository.findAllById(anyList())).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(List.of(500L))).thenReturn(List.of(productImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/trade-cancel");
        }

        @Test
        @DisplayName("TRADE_COMPLETE 와 TRADE_CANCEL 이 혼재하면 productImageMap 을 공유해 각자 올바른 이미지로 매핑된다")
        void it_shares_product_image_map_for_trade_complete_and_trade_cancel_alerts() {
            Alert tcAlert = mockAlert(1L, AlertType.TRADE_COMPLETE, 100L, "거래 완료", "광산이 차감되었습니다.");
            Alert cancelAlert = mockAlert(2L, AlertType.TRADE_CANCEL, 400L, "거래 철회 완료", "거래가 철회되었습니다.");

            Product tcProduct = mock(Product.class);
            when(tcProduct.getId()).thenReturn(100L);
            Image tcImage = mock(Image.class);
            when(tcImage.getImageUrl()).thenReturn("http://img/tc");
            ProductImage tcProductImage = mock(ProductImage.class);
            when(tcProductImage.getProduct()).thenReturn(tcProduct);
            when(tcProductImage.getImage()).thenReturn(tcImage);

            Product cancelProduct = mock(Product.class);
            when(cancelProduct.getId()).thenReturn(500L);
            Image cancelImage = mock(Image.class);
            when(cancelImage.getImageUrl()).thenReturn("http://img/cancel");
            ProductImage cancelProductImage = mock(ProductImage.class);
            when(cancelProductImage.getProduct()).thenReturn(cancelProduct);
            when(cancelProductImage.getImage()).thenReturn(cancelImage);

            TradeComplete tradeComplete = mock(TradeComplete.class);
            when(tradeComplete.getProduct()).thenReturn(cancelProduct);
            TradeCancel tradeCancel = mock(TradeCancel.class);
            when(tradeCancel.getId()).thenReturn(400L);
            when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(tcAlert, cancelAlert));
            when(tradeCancelRepository.findAllById(anyList())).thenReturn(List.of(tradeCancel));
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of(tcProductImage, cancelProductImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/tc");
            assertThat(result.get(1).images()).hasSize(1);
            assertThat(result.get(1).images().get(0).imageUrl()).isEqualTo("http://img/cancel");
        }

        @Test
        @DisplayName("OTHER_MEMBER_TRADE_COMPLETE 타입 알림에 상품 이미지가 있으면 이미지 URL 을 반환한다")
        void it_returns_product_image_url_for_other_member_trade_complete_alert() {
            Alert alert = mockAlert(25L, AlertType.OTHER_MEMBER_TRADE_COMPLETE, 150L, "거래 완료 알림", "구매자님이 거래를 완료하였습니다.");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(150L);
            Image image = mock(Image.class);
            when(image.getImageUrl()).thenReturn("http://img/other");
            ProductImage productImage = mock(ProductImage.class);
            when(productImage.getId()).thenReturn(5L);
            when(productImage.getProduct()).thenReturn(product);
            when(productImage.getImage()).thenReturn(image);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert));
            when(productImageRepository.findAllByProductIdIn(List.of(150L))).thenReturn(List.of(productImage));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo(AlertType.OTHER_MEMBER_TRADE_COMPLETE);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/other");
        }

        @Test
        @DisplayName("sourceId 가 다른 TRADE_COMPLETE 알림 2개는 각자 올바른 이미지로 매핑된다")
        void it_maps_product_images_to_correct_alert_by_source_id() {
            Alert alert1 = mockAlert(1L, AlertType.TRADE_COMPLETE, 100L, "거래 완료1", "광산이 차감되었습니다.");
            Alert alert2 = mockAlert(2L, AlertType.TRADE_COMPLETE, 200L, "거래 완료2", "광산이 차감되었습니다.");

            Product product1 = mock(Product.class);
            when(product1.getId()).thenReturn(100L);
            Image image1 = mock(Image.class);
            when(image1.getImageUrl()).thenReturn("http://img/100");
            ProductImage productImage1 = mock(ProductImage.class);
            when(productImage1.getProduct()).thenReturn(product1);
            when(productImage1.getImage()).thenReturn(image1);

            Product product2 = mock(Product.class);
            when(product2.getId()).thenReturn(200L);
            Image image2 = mock(Image.class);
            when(image2.getImageUrl()).thenReturn("http://img/200");
            ProductImage productImage2 = mock(ProductImage.class);
            when(productImage2.getProduct()).thenReturn(product2);
            when(productImage2.getImage()).thenReturn(image2);

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(alert1, alert2));
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of(productImage1, productImage2));

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).images()).hasSize(1);
            assertThat(result.get(0).images().get(0).imageUrl()).isEqualTo("http://img/100");
            assertThat(result.get(1).images()).hasSize(1);
            assertThat(result.get(1).images().get(0).imageUrl()).isEqualTo("http://img/200");
        }

        @Test
        @DisplayName("NOTICE 와 REPORT 알림이 혼재하면 각각 올바른 이미지 저장소에서 이미지를 조회한다")
        void it_fetches_images_from_correct_repositories_for_mixed_alert_types() {
            Alert noticeAlert = mockAlert(70L, AlertType.NOTICE, 600L, "공지", "공지 내용");
            Alert reportAlert = mockAlert(80L, AlertType.REPORT, 700L, "신고", "신고 내용");

            when(alertReceiptRepository.findByMemberId(1L)).thenReturn(List.of(noticeAlert, reportAlert));
            when(reportImageRepository.findAllByReportIdIn(anyList())).thenReturn(List.of());

            List<GetAlertResponse> result = service.execute();

            assertThat(result).hasSize(2);
            verify(noticeImageRepository).findAllByNoticeIdIn(anyList());
            verify(reportImageRepository).findAllByReportIdIn(anyList());
        }
    }
}
