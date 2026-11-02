package team.startup.gwangsan.domain.alert.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Alert 문구 팩토리는 기존 CreateAlertServiceImpl 문구를 유지한다")
class AlertTest {

    @Test
    @DisplayName("거래 관련 알림")
    void it_keeps_trade_texts() {
        assertThat(Alert.tradeCompleteForBuyer(1L, 2L, "상품"))
                .isEqualTo(new Alert(null, 1L, 2L, AlertType.TRADE_COMPLETE, "상품", "광산이 차감되었습니다.", null));
        assertThat(Alert.tradeCompleteForSeller(1L, 3L, "상품").content()).isEqualTo("광산이 추가되었습니다.");
        assertThat(Alert.tradeCompleteReject(1L, "상품"))
                .isEqualTo(new Alert(null, 1L, null, AlertType.TRADE_COMPLETE_REJECT, "상품", "거래 완료가 거절되었습니다.", null));
        assertThat(Alert.otherMemberTradeComplete(1L, 2L, "상품", "구매자"))
                .isEqualTo(new Alert(null, 1L, 2L, AlertType.OTHER_MEMBER_TRADE_COMPLETE, "상품", "구매자님이 거래를 완료하였습니다.", null));
        assertThat(Alert.tradeCancel(7L, 8L))
                .isEqualTo(new Alert(null, 7L, 8L, AlertType.TRADE_CANCEL, "거래 철회 완료", "거래가 철회되었습니다.", null));
        assertThat(Alert.tradeCancelReject(7L, 8L))
                .isEqualTo(new Alert(null, 7L, 8L, AlertType.TRADE_CANCEL_REJECT, "거래 철회 기각", "거래 철회 요청이 기각되었습니다.", null));
    }

    @Test
    @DisplayName("거래 완료 요청·철회 완료·문의 답변은 업무 트랜잭션에 저장한다")
    void it_limits_atomic_record_scope() {
        assertThat(java.util.Arrays.stream(AlertType.values()).filter(AlertType::isRecordedWithBusinessTransaction))
                .containsExactlyInAnyOrder(AlertType.OTHER_MEMBER_TRADE_COMPLETE, AlertType.TRADE_CANCEL, AlertType.FEEDBACK_ANSWER);
    }

    @Test
    @DisplayName("회원·신고·공지 알림")
    void it_keeps_other_texts() {
        assertThat(Alert.recommender(5L, 5000))
                .isEqualTo(new Alert(null, 5L, 5L, AlertType.RECOMMENDER, "추천인 등록", "추천인으로 등록되어 5000 광산이 지급되었습니다.", null));
        assertThat(Alert.review(3L, 4L, "작성자"))
                .isEqualTo(new Alert(null, 3L, 4L, AlertType.REVIEW, "작성자리뷰 등록", "나에 대한 후기가 등록되었습니다.", null));
        assertThat(Alert.reportReject(3L, 4L, "피신고자"))
                .isEqualTo(new Alert(null, 3L, 4L, AlertType.REPORT_REJECT, "신고 기각", "피신고자님에 대한 신고가 기각되었습니다.", null));
        assertThat(Alert.report(3L, 4L, 7))
                .isEqualTo(new Alert(null, 3L, 4L, AlertType.REPORT, "신고 결과", "7일 정지 조치되었습니다.", null));
        assertThat(Alert.notice(9L, "공지 제목"))
                .isEqualTo(new Alert(null, 9L, null, AlertType.NOTICE, "공지 제목", "공지가 등록되었습니다.", null));
    }
}
