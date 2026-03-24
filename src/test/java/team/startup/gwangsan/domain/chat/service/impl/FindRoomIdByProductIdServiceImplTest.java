package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomIdResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRoomIdByProductIdServiceImpl 단위 테스트")
class FindRoomIdByProductIdServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private FindRoomIdByProductIdServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member member;

        @BeforeEach
        void setUp() {
            member = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
        }

        @Test
        @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            when(chatRoomRepository.findByProductIdAndMember(10L, member)).thenReturn(Optional.empty());

            assertThrows(NotFoundChatRoomException.class, () -> service.execute(10L));
        }

        @Test
        @DisplayName("채팅방이 있으면 roomId 를 반환한다")
        void it_returns_room_id_when_room_found() {
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(42L);
            when(chatRoomRepository.findByProductIdAndMember(10L, member)).thenReturn(Optional.of(chatRoom));

            GetRoomIdResponse response = service.execute(10L);

            assertThat(response.roomId()).isEqualTo(42L);
        }
    }
}
