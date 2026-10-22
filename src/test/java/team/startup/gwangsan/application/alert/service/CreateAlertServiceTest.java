package team.startup.gwangsan.application.alert.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.alert.data.CreateAlertForMembersCommand;
import team.startup.gwangsan.application.alert.data.CreateAlertFromSourceCommand;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReportSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReviewSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCancelSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCompleteSource;
import team.startup.gwangsan.application.alert.port.out.SaveAlertPort;
import team.startup.gwangsan.domain.alert.exception.UnsupportedAlertTypeException;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAlertService")
class CreateAlertServiceTest {

    private static final Long MEMBER = 10L;

    @Mock
    private SaveAlertPort saveAlertPort;
    @Mock
    private LoadAlertSourcePort source;
    @InjectMocks
    private CreateAlertService service;

    private void create(AlertType type, Long sourceId) {
        service.create(new CreateAlertFromSourceCommand(type, sourceId, MEMBER, null));
    }

    @Nested
    @DisplayName("record() 는")
    class Describe_record {

        @Test
        @DisplayName("전달받은 알림과 수신자를 그대로 저장하고 ID 를 돌려준다")
        void it_saves_given_alert() {
            Alert alert = Alert.tradeCancel(1L, 2L);
            when(saveAlertPort.save(alert, List.of(3L, 4L))).thenReturn(99L);

            assertThat(service.record(new RecordAlertCommand(alert, List.of(3L, 4L)))).isEqualTo(99L);
            verifyNoInteractions(source);
        }

        @Test
        @DisplayName("거래 완료 요청 알림도 동기 저장 대상이다")
        void it_accepts_other_member_trade_complete() {
            Alert alert = Alert.otherMemberTradeComplete(5L, 3L, "상품", "구매자");

            service.record(new RecordAlertCommand(alert, List.of(2L)));

            verify(saveAlertPort).save(alert, List.of(2L));
        }

        @Test
        @DisplayName("확정 범위 밖 유형(거래 확정·후기·공지 등)은 거절하고 저장하지 않는다")
        void it_rejects_types_outside_atomic_scope() {
            for (Alert alert : List.of(
                    Alert.tradeCompleteForBuyer(1L, 2L, "상품"),
                    Alert.tradeCancelReject(1L, 2L),
                    Alert.review(1L, 2L, "작성자"),
                    Alert.recommender(1L, 5000),
                    Alert.report(1L, 2L, 3),
                    Alert.notice(1L, "공지"))) {
                assertThatThrownBy(() -> service.record(new RecordAlertCommand(alert, List.of(1L))))
                        .as(alert.type().name())
                        .isInstanceOf(UnsupportedAlertTypeException.class);
            }
            verifyNoInteractions(saveAlertPort);
        }
    }

    @Nested
    @DisplayName("create() 는")
    class Describe_create {

        @Test
        @DisplayName("회원이 없으면 원천 조회 예외를 그대로 던지고 저장하지 않는다")
        void it_propagates_member_not_found() {
            doThrow(new NotFoundMemberException()).when(source).requireMember(MEMBER);

            assertThatThrownBy(() -> create(AlertType.REVIEW, 1L)).isInstanceOf(NotFoundMemberException.class);
            verifyNoInteractions(saveAlertPort);
        }

        @Test
        @DisplayName("TRADE_COMPLETE 는 구매자·판매자에게 각각 알림을 저장한다")
        void it_saves_trade_complete_for_both() {
            when(source.tradeComplete(1L)).thenReturn(new TradeCompleteSource(5L, "상품", 2L, 3L, "구매자"));

            create(AlertType.TRADE_COMPLETE, 1L);

            InOrder order = inOrder(saveAlertPort);
            order.verify(saveAlertPort).save(Alert.tradeCompleteForBuyer(5L, 3L, "상품"), List.of(2L));
            order.verify(saveAlertPort).save(Alert.tradeCompleteForSeller(5L, 2L, "상품"), List.of(3L));
        }

        @Test
        @DisplayName("TRADE_COMPLETE_REJECT 는 활성 상품 제목으로 저장한다")
        void it_saves_trade_complete_reject() {
            when(source.activeProductTitle(1L)).thenReturn("상품");

            create(AlertType.TRADE_COMPLETE_REJECT, 1L);

            verify(saveAlertPort).save(Alert.tradeCompleteReject(1L, "상품"), List.of(MEMBER));
        }

        @Test
        @DisplayName("OTHER_MEMBER_TRADE_COMPLETE 는 상품 ID 를 sourceId 로, 요청 대상에게 저장한다")
        void it_saves_other_member_trade_complete() {
            when(source.tradeComplete(1L)).thenReturn(new TradeCompleteSource(5L, "상품", 2L, 3L, "구매자"));

            create(AlertType.OTHER_MEMBER_TRADE_COMPLETE, 1L);

            verify(saveAlertPort).save(Alert.otherMemberTradeComplete(5L, 3L, "상품", "구매자"), List.of(MEMBER));
        }

        @Test
        @DisplayName("RECOMMENDER 는 가입 회원 존재를 확인하고 보상 금액을 본문에 넣는다")
        void it_saves_recommender() {
            when(source.recommenderReward()).thenReturn(5000);

            create(AlertType.RECOMMENDER, 7L);

            verify(source).requireMember(7L);
            verify(saveAlertPort).save(Alert.recommender(7L, 5000), List.of(MEMBER));
        }

        @Test
        @DisplayName("REVIEW 와 REPORT_REJECT 는 원천 정보를 문구에 넣는다")
        void it_saves_review_and_report_reject() {
            when(source.review(1L)).thenReturn(new ReviewSource(4L, "작성자"));
            when(source.report(2L)).thenReturn(new ReportSource(5L, "피신고자"));

            create(AlertType.REVIEW, 1L);
            create(AlertType.REPORT_REJECT, 2L);

            verify(saveAlertPort).save(Alert.review(1L, 4L, "작성자"), List.of(MEMBER));
            verify(saveAlertPort).save(Alert.reportReject(2L, 5L, "피신고자"), List.of(MEMBER));
        }

        @Test
        @DisplayName("TRADE_CANCEL 은 memberId 가 아니라 거래 양측에게 receipt 를 만든다")
        void it_saves_trade_cancel_for_both_parties() {
            when(source.tradeCancelWithParties(1L)).thenReturn(new TradeCancelSource(2L, 3L, 4L));

            create(AlertType.TRADE_CANCEL, 1L);

            verify(saveAlertPort).save(Alert.tradeCancel(1L, 2L), List.of(3L, 4L));
        }

        @Test
        @DisplayName("TRADE_CANCEL_REJECT 는 철회 요청자를 발신자로 저장한다")
        void it_saves_trade_cancel_reject() {
            when(source.tradeCancelRequester(1L)).thenReturn(2L);

            create(AlertType.TRADE_CANCEL_REJECT, 1L);

            verify(saveAlertPort).save(Alert.tradeCancelReject(1L, 2L), List.of(MEMBER));
        }

        @Test
        @DisplayName("REPORT 는 신고와 정지 일수를 조회해 저장한다")
        void it_saves_report() {
            when(source.report(1L)).thenReturn(new ReportSource(5L, "피신고자"));
            when(source.suspendedDays(9L)).thenReturn(3);

            service.create(new CreateAlertFromSourceCommand(AlertType.REPORT, 1L, MEMBER, 9L));

            verify(saveAlertPort).save(Alert.report(1L, 5L, 3), List.of(MEMBER));
        }

        @Test
        @DisplayName("FEEDBACK_ANSWER 는 원천 조회 없이 고정 문구로 저장한다")
        void it_saves_feedback_answer() {
            create(AlertType.FEEDBACK_ANSWER, 1L);

            verify(saveAlertPort).save(Alert.feedbackAnswer(1L), List.of(MEMBER));
        }

        @Test
        @DisplayName("NOTICE 는 단건 경로에서 지원하지 않는다")
        void it_rejects_notice() {
            assertThatThrownBy(() -> create(AlertType.NOTICE, 1L)).isInstanceOf(UnsupportedAlertTypeException.class);
            verifyNoInteractions(saveAlertPort);
        }
    }

    @Nested
    @DisplayName("createForMembers() 는")
    class Describe_createForMembers {

        @Test
        @DisplayName("존재하는 회원에게만 공지 알림 receipt 를 만든다")
        void it_saves_notice_for_existing_members() {
            when(source.existingMemberIds(List.of(1L, 2L, 3L))).thenReturn(List.of(1L, 3L));
            when(source.noticeTitle(8L)).thenReturn("공지");

            service.createForMembers(new CreateAlertForMembersCommand(AlertType.NOTICE, 8L, List.of(1L, 2L, 3L)));

            verify(saveAlertPort).save(Alert.notice(8L, "공지"), List.of(1L, 3L));
        }

        @Test
        @DisplayName("회원이 없거나 NOTICE 가 아니면 아무것도 저장하지 않는다")
        void it_skips_without_members_or_other_type() {
            when(source.existingMemberIds(any())).thenReturn(List.of()).thenReturn(List.of(1L));

            service.createForMembers(new CreateAlertForMembersCommand(AlertType.NOTICE, 8L, List.of(1L)));
            service.createForMembers(new CreateAlertForMembersCommand(AlertType.REVIEW, 8L, List.of(1L)));

            verify(source, never()).noticeTitle(any());
            verifyNoInteractions(saveAlertPort);
        }
    }
}
