package team.startup.gwangsan.domain.chat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoom 단위 테스트")
class ChatRoomTest {

    private static final Long BUYER = 1L;
    private static final Long SELLER = 2L;
    private static final Long STRANGER = 3L;
    private static final LocalDateTime AT = LocalDateTime.of(2026, 1, 1, 12, 0);

    private final ChatRoom room = new ChatRoom(10L, 20L, BUYER, SELLER, null, null);

    @Nested
    @DisplayName("참여자 메서드는")
    class Describe_participants {

        @Test
        @DisplayName("it_buyer·seller 만 참여자로 보고 각자의 상대를 반환한다")
        void it_identifies_participants_and_other_member() {
            assertThat(room.isParticipant(BUYER)).isTrue();
            assertThat(room.isParticipant(SELLER)).isTrue();
            assertThat(room.isParticipant(STRANGER)).isFalse();
            assertThat(room.otherMemberId(BUYER)).isEqualTo(SELLER);
            assertThat(room.otherMemberId(SELLER)).isEqualTo(BUYER);
            // 기존 Entity 와 같이 비참여자의 상대는 구매자다.
            assertThat(room.otherMemberId(STRANGER)).isEqualTo(BUYER);
            assertThat(room.isSeller(SELLER)).isTrue();
            assertThat(room.isSeller(BUYER)).isFalse();
        }
    }

    @Nested
    @DisplayName("숨김 상태는")
    class Describe_hidden {

        @Test
        @DisplayName("it_참여자별로 기록·해제하고 비참여자는 바꾸지 않는다")
        void it_hides_and_unhides_per_participant() {
            ChatRoom hiddenByBuyer = room.hideFor(BUYER, AT);
            assertThat(hiddenByBuyer.isHiddenFor(BUYER)).isTrue();
            assertThat(hiddenByBuyer.isHiddenFor(SELLER)).isFalse();
            assertThat(hiddenByBuyer.hiddenByBuyerAt()).isEqualTo(AT);

            ChatRoom both = hiddenByBuyer.hideFor(SELLER, AT.plusHours(1));
            assertThat(both.hiddenBySellerAt()).isEqualTo(AT.plusHours(1));
            assertThat(both.unhideFor(BUYER).isHiddenFor(BUYER)).isFalse();
            assertThat(both.unhideFor(BUYER).isHiddenFor(SELLER)).isTrue();

            assertThat(room.hideFor(STRANGER, AT)).isSameAs(room);
            assertThat(both.isHiddenFor(STRANGER)).isFalse();
        }
    }
}
