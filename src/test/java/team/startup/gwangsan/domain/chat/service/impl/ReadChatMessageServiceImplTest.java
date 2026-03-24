package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadChatMessageServiceImpl 단위 테스트")
class ReadChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private ReadChatMessageServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("현재 사용자 ID 로 메시지 읽음 처리를 호출한다")
        void it_calls_readMessage_with_current_member_id() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(5L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            service.execute(10L, 20L);

            verify(chatMessageRepository).readMessage(10L, 20L, 5L);
        }
    }
}
