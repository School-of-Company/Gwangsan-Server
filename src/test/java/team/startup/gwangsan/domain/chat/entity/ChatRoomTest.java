package team.startup.gwangsan.domain.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.member.entity.Member;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ChatRoom 단위 테스트")
class ChatRoomTest {

    @Nested
    @DisplayName("isHiddenFor() 메서드는")
    class Describe_isHiddenFor {

        @Test
        @DisplayName("참여자별 hidden 상태를 구분하고 비참여자는 false를 반환한다")
        void it_checks_hidden_state_for_each_participant_only() {
            Member buyer = mock(Member.class);
            Member seller = mock(Member.class);
            Member outsider = mock(Member.class);
            when(buyer.getId()).thenReturn(1L);
            when(seller.getId()).thenReturn(2L);
            when(outsider.getId()).thenReturn(3L);
            ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller).build();
            LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

            assertThat(room.isHiddenFor(buyer)).isFalse();
            assertThat(room.isHiddenFor(seller)).isFalse();
            room.hideFor(buyer, now);
            assertThat(room.isHiddenFor(buyer)).isTrue();
            assertThat(room.isHiddenFor(seller)).isFalse();
            room.hideFor(seller, now);
            assertThat(room.isHiddenFor(seller)).isTrue();
            assertThat(room.isHiddenFor(outsider)).isFalse();
            room.unhideFor(buyer);
            assertThat(room.isHiddenFor(buyer)).isFalse();
            assertThat(room.isHiddenFor(seller)).isTrue();
            room.unhideFor(seller);
            assertThat(room.isHiddenFor(seller)).isFalse();
        }
    }
}
