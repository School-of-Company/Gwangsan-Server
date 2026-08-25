package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteChatRoomServiceImpl 단위 테스트")
class DeleteChatRoomServiceImplTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;

    @InjectMocks
    private DeleteChatRoomServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("채팅방이 존재하지 않으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            Member member = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("요청자가 채팅방의 참여자가 아니면 NotFoundChatRoomException 을 던지고 숨김 처리하지 않는다")
        void it_throws_NotFoundChatRoomException_when_member_is_not_participant() {
            Member member = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.isParticipant(member)).thenReturn(false);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));

            assertThatThrownBy(() -> service.execute(10L))
                    .isInstanceOf(NotFoundChatRoomException.class);

            verify(chatRoom, never()).hideFor(any(), any());
        }

        @Test
        @DisplayName("요청자가 참여자면 요청자 쪽만 숨김 처리한다")
        void it_hides_room_only_for_requester() {
            Member member = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.isParticipant(member)).thenReturn(true);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));

            service.execute(10L);

            verify(chatRoom).hideFor(eq(member), any(LocalDateTime.class));
        }
    }
}
