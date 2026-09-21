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
    @DisplayName("참여자 메서드는")
    class Describe_participants {

        @Test
        @DisplayName("실제 buyer·seller ID를 참여자로 판단하고 각 참여자의 상대를 반환한다")
        void it_identifies_actual_participants_and_returns_the_other_participant() {
            Member buyer = member(1L, "구매자");
            Member seller = member(2L, "판매자");
            Member outsider = member(3L, "외부인");
            ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller).build();

            assertThat(room.isParticipant(buyer)).isTrue();
            assertThat(room.isParticipant(seller)).isTrue();
            assertThat(room.isParticipant(outsider)).isFalse();
            assertThat(room.getOtherMember(buyer)).isSameAs(seller);
            assertThat(room.getOtherMember(seller)).isSameAs(buyer);
        }
    }

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

    private static Member member(Long id, String nickname) {
        Member member = Member.builder()
                .name(nickname).nickname(nickname).phoneNumber(id + "0000000000").password("password").build();
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
