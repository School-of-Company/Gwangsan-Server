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
        @DisplayName("TRADE_COMPLETE 타입 알림이 있으면 상품 이미지를 포함해 반환한다")
        void it_returns_trade_complete_alert_with_product_images() {
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
