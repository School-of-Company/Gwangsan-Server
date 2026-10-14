package team.startup.gwangsan.domain.notification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PushMessage")
class PushMessageTest {

    @Nested
    @DisplayName("byType() 는")
    class Describe_byType {

        @Test
        @DisplayName("기존 유형별 본문과 기본 제목을 쓴다")
        void it_uses_existing_bodies() {
            assertThat(PushMessage.byType(NotificationType.NOTICE, 1L))
                    .isEqualTo(new PushMessage("시민화폐 광산", "새로운 공지가 등록되었습니다.", NotificationType.NOTICE, 1L));
            assertThat(PushMessage.byType(NotificationType.CHATTING, 2L).body()).isEqualTo("새로운 메세지가 도착했습니다.");
            assertThat(PushMessage.byType(NotificationType.RECOMMENDATION, 3L).body()).isEqualTo("추천인이 등록되었습니다.");
            assertThat(PushMessage.byType(NotificationType.TRADE_COMPLETE, 4L).body()).isEqualTo("거래가 승인되었습니다.");
        }

        @Test
        @DisplayName("예약 유형은 상대방 이름이 필요하므로 IllegalArgumentException 을 던진다")
        void it_rejects_reservation_types() {
            assertThatThrownBy(() -> PushMessage.byType(NotificationType.RESERVATION_REMINDER, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PushMessage.byType(NotificationType.RESERVATION_CANCEL, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("reservation() 은 쉼표가 있는 기존 예약 제목을 쓴다")
    void it_uses_reservation_title() {
        assertThat(PushMessage.reservation(NotificationType.RESERVATION_CANCEL, 9L, "본문").title()).isEqualTo("시민화폐, 광산");
    }

    @Test
    @DisplayName("발송 확인이 필요한 유형은 예약 리마인더뿐이다")
    void it_requires_confirmation_only_for_reminder() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(type.requiresDeliveryConfirmation()).as(type.name())
                    .isEqualTo(type == NotificationType.RESERVATION_REMINDER);
        }
    }
}
