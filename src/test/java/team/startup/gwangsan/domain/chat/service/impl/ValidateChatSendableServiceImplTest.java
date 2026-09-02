package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateChatSendableServiceImpl 단위 테스트")
class ValidateChatSendableServiceImplTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private BlockValidator blockValidator;

    @InjectMocks
    private ValidateChatSendableServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("채팅방이 존재하지 않으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            when(memberUtil.getCurrentMember()).thenReturn(mock(Member.class));
            when(chatRoomRepository.findChatRoomByRoomId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(NotFoundChatRoomException.class);

            verifyNoInteractions(blockValidator);
        }

        @Test
        @DisplayName("요청자가 채팅방의 참여자가 아니면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_member_is_not_participant() {
            Member member = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(10L))
                    .isInstanceOf(NotFoundChatRoomException.class);

            verifyNoInteractions(blockValidator);
        }

        @Test
        @DisplayName("상대방과 차단 관계면 BlockedMemberException 을 던진다")
        void it_throws_BlockedMemberException_when_blocked() {
            Member member = mock(Member.class);
            Member otherMember = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(true);
            when(chatRoom.getOtherMember(member)).thenReturn(otherMember);
            doThrow(new BlockedMemberException()).when(blockValidator).validate(member, otherMember);

            assertThatThrownBy(() -> service.execute(10L))
                    .isInstanceOf(BlockedMemberException.class);
        }

        @Test
        @DisplayName("참여자이고 차단 관계가 없으면 예외 없이 통과한다")
        void it_passes_when_participant_and_not_blocked() {
            Member member = mock(Member.class);
            Member otherMember = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(true);
            when(chatRoom.getOtherMember(member)).thenReturn(otherMember);

            assertThatCode(() -> service.execute(10L)).doesNotThrowAnyException();

            verify(blockValidator).validate(member, otherMember);
        }
    }
}
