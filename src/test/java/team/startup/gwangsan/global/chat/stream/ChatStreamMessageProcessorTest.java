package team.startup.gwangsan.global.chat.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatStreamMessageProcessor 단위 테스트")
class ChatStreamMessageProcessorTest {

    @Mock
    private ChatStreamHandler handler;

    @Mock
    private ChatStreamRedisAdapter redisAdapter;

    @Mock
    private ChatStreamProperties props;

    private ChatStreamMessageProcessor processor;

    private final String streamKey = "chat:room:42:messages";
    private final String retryKey = "chat:retry:messages";
    private final String dlqKey = "chat:dlq:messages";

    @BeforeEach
    void setUp() {
        when(props.getGroup()).thenReturn("chat-message-persistors");
        when(props.getRetryKey()).thenReturn(retryKey);
        when(props.getDlqKey()).thenReturn(dlqKey);
        when(props.getRetryMax()).thenReturn(5);

        processor = new ChatStreamMessageProcessor(List.of(handler), redisAdapter, props);
    }

    @Nested
    @DisplayName("페이로드 파싱은")
    class Describe_payload_parsing {

        @Test
        @DisplayName("필수 필드가 모두 있으면 핸들러를 호출하고 ACK한다")
        void it_calls_handler_when_payload_valid() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "roomId", "42", "senderId", "7", "content", "안녕하세요", "messageType", "TEXT")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 0);

            verify(handler).handle(argThat(m ->
                    m.messageId().equals(999L) &&
                    m.roomId().equals(42L) &&
                    m.senderId().equals(7L) &&
                    m.content().equals("안녕하세요") &&
                    m.messageType() == MessageType.TEXT
            ));
            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
        }

        @Test
        @DisplayName("roomId가 없으면 DLQ로 보내고 ACK한다")
        void it_sends_to_dlq_when_roomId_missing() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "senderId", "7", "content", "hello")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 0);

            verify(redisAdapter).sendToDlq(eq(streamKey), eq(record), any());
            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
            verifyNoInteractions(handler);
        }

        @Test
        @DisplayName("content가 비어있으면 DLQ로 보내고 ACK한다")
        void it_sends_to_dlq_when_content_blank() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "roomId", "42", "senderId", "7", "content", "   ")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 0);

            verify(redisAdapter).sendToDlq(eq(streamKey), eq(record), any());
            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
            verifyNoInteractions(handler);
        }
    }

    @Nested
    @DisplayName("핸들러 실패 처리는")
    class Describe_handler_failure {

        @Test
        @DisplayName("attempt >= retryMax이면 DLQ로 보내고 ACK한다")
        void it_sends_to_dlq_when_max_retry_exceeded() {
            doThrow(new RuntimeException("DB 연결 실패")).when(handler).handle(any(ChatStreamMessage.class));

            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "roomId", "42", "senderId", "7", "content", "hello", "attempt", "5")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 5);

            verify(redisAdapter).sendToDlq(eq(streamKey), eq(record), any());
            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
        }

        @Test
        @DisplayName("attempt < retryMax이면 retry stream으로 보내고 ACK한다")
        void it_sends_to_retry_when_attempt_below_max() {
            doThrow(new RuntimeException("일시적 오류")).when(handler).handle(any(ChatStreamMessage.class));

            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "roomId", "42", "senderId", "7", "content", "hello")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 0);

            ArgumentCaptor<Integer> attemptCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(redisAdapter).sendToRetry(eq(streamKey), eq(record), attemptCaptor.capture(), any());
            assertThat(attemptCaptor.getValue()).isEqualTo(1);
            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
        }

        @Test
        @DisplayName("정상 처리 시 ACK하고 retry/dlq를 호출하지 않는다")
        void it_acks_on_success() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of("messageId", "999", "roomId", "1", "senderId", "2", "content", "hi")
            ).withId(RecordId.of("1700000000000-0"));

            processor.process(streamKey, record, 0);

            verify(redisAdapter).ack(eq(streamKey), any(RecordId.class));
            verify(redisAdapter, never()).sendToRetry(any(), any(), anyInt(), any());
            verify(redisAdapter, never()).sendToDlq(any(), any(), any());
        }
    }
}
