package team.startup.gwangsan.global.chat.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.service.SaveChatMessageService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaveChatMessageHandler 단위 테스트")
class SaveChatMessageHandlerTest {

    @InjectMocks private SaveChatMessageHandler handler;
    @Mock private SaveChatMessageService saveChatMessageService;

    @Nested
    @DisplayName("handle() 메서드는")
    class Describe_handle {

        @Nested
        @DisplayName("ChatStreamMessage가 주어졌을 때")
        class Context_with_chat_stream_message {

            @Test
            @DisplayName("saveChatMessageService.execute()를 올바른 인자로 호출한다")
            void it_calls_save_chat_message_service_with_correct_args() {
                LocalDateTime createdAt = LocalDateTime.now();
                ChatStreamMessage message = new ChatStreamMessage(
                        1L, 2L, "안녕", List.of(), MessageType.TEXT, 3L, createdAt
                );

                handler.handle(message);

                verify(saveChatMessageService).execute(
                        eq(1L), eq(2L), eq("안녕"), eq(List.of()),
                        eq(MessageType.TEXT), eq(3L), eq(createdAt)
                );
            }
        }
    }
}
