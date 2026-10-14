package team.startup.gwangsan.domain.post.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("순수 예약 모델")
class ProductReservationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);

    private static ProductReservation reserve(LocalDateTime scheduledAt) {
        return ProductReservation.reserve(1L, 20L, scheduledAt, "광산구청", "광주 광산구", 35.1, 126.7, NOW);
    }

    @Nested
    @DisplayName("reserve 는")
    class Describe_reserve {

        @Test
        @DisplayName("30분 이내 약속이면 30분 전 알림을 보낸 것으로 표시한다(경계 포함)")
        void it_marks_reminder30_within_thirty_minutes() {
            assertThat(reserve(NOW.plusMinutes(30)).getReminder30SentAt()).isEqualTo(NOW);
            assertThat(reserve(NOW.plusMinutes(5)).getReminder30SentAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("30분 넘게 남았으면 표시하지 않고 PENDING 으로 만든다")
        void it_leaves_reminder30_for_later() {
            ProductReservation reservation = reserve(NOW.plusMinutes(31));
            assertThat(reservation.getReminder30SentAt()).isNull();
            assertThat(reservation.getReminderAtSentAt()).isNull();
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(reservation.getReserverId()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("cancel 은")
    class Describe_cancel {

        @Test
        @DisplayName("상품 작성자나 예약자만 취소할 수 있다")
        void it_allows_participants_only() {
            ProductReservation byOwner = reserve(NOW.plusHours(1));
            byOwner.cancel(10L, 10L);
            assertThat(byOwner.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

            ProductReservation byReserver = reserve(NOW.plusHours(1));
            byReserver.cancel(20L, 10L);
            assertThat(byReserver.isPending()).isFalse();

            ProductReservation byStranger = reserve(NOW.plusHours(1));
            assertThatThrownBy(() -> byStranger.cancel(99L, 10L))
                    .isInstanceOf(ReservationParticipantOnlyException.class);
            assertThat(byStranger.isPending()).isTrue();
        }
    }

    @Nested
    @DisplayName("reminder 기한은")
    class Describe_reminder_due {

        @Test
        @DisplayName("30분 전 알림은 약속 30분 전부터, 정시 알림은 약속 시각부터이며 보낸 뒤에는 다시 오지 않는다")
        void it_is_due_from_boundary_until_sent() {
            ProductReservation reservation = new ProductReservation(1L, 1L, 20L, ReservationStatus.PENDING,
                    NOW.plusMinutes(30), null, null, null, null, null, null);

            assertThat(reservation.isReminder30Due(NOW.minusSeconds(1))).isFalse();
            assertThat(reservation.isReminder30Due(NOW)).isTrue();
            assertThat(reservation.isReminderAtDue(NOW)).isFalse();
            assertThat(reservation.isReminderAtDue(NOW.plusMinutes(30))).isTrue();

            reservation.markReminder30Sent(NOW);
            reservation.markReminderAtSent(NOW.plusMinutes(30));
            assertThat(reservation.isReminder30Due(NOW.plusHours(1))).isFalse();
            assertThat(reservation.isReminderAtDue(NOW.plusHours(1))).isFalse();
        }
    }

    @Nested
    @DisplayName("거래 완료 연동은")
    class Describe_trade_completion {

        @Test
        @DisplayName("예약자가 구매자·판매자 중 한 명이 아니면 ReservationParticipantOnlyException, 완료하면 COMPLETED")
        void it_checks_participant_and_completes() {
            ProductReservation reservation = reserve(NOW.plusHours(2));

            reservation.requireParticipant(1L, reservation.getReserverId());
            assertThatThrownBy(() -> reservation.requireParticipant(1L, 2L))
                    .isInstanceOf(ReservationParticipantOnlyException.class);
            reservation.complete();
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }
    }
}
