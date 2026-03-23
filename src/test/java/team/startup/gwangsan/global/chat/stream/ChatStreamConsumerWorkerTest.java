package team.startup.gwangsan.global.chat.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatStreamConsumerWorker 단위 테스트")
class ChatStreamConsumerWorkerTest {

    @Mock
    private ChatStreamRedisAdapter redisAdapter;

    @Mock
    private ChatStreamMessageProcessor processor;

    @Mock
    private ChatStreamProperties props;

    private ChatStreamConsumerWorker worker;

    private final String streamKey = "chat:room:42:messages";
    private final String retryKey = "chat:retry:messages";

    @BeforeEach
    void setUp() {
        when(props.getRetryKey()).thenReturn(retryKey);
        when(redisAdapter.getKnownStreamKeys()).thenReturn(Set.of(streamKey));
        worker = new ChatStreamConsumerWorker(redisAdapter, processor, props);
    }

    @Nested
    @DisplayName("consumeMessages()는")
    class Describe_consumeMessages {

        @Test
        @DisplayName("refreshStreamKeys, ensureGroupExists, readMessages를 호출한다")
        void it_delegates_to_adapter() {
            when(redisAdapter.readMessages(anyString())).thenReturn(null);

            worker.consumeMessages();

            verify(redisAdapter).refreshStreamKeys();
            verify(redisAdapter, atLeastOnce()).ensureGroupExists(anyString());
            verify(redisAdapter, atLeastOnce()).readMessages(anyString());
        }

        @Test
        @DisplayName("record가 있으면 processor.process()를 호출한다")
        void it_calls_processor_when_records_exist() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of(
                            "messageId", "999",
                            "roomId", "42",
                            "senderId", "7",
                            "content", "안녕하세요",
                            "messageType", "TEXT"
                    )
            ).withId(RecordId.of("1700000000000-0"));

            when(redisAdapter.readMessages(streamKey)).thenReturn(List.of(record));
            when(redisAdapter.readMessages(retryKey)).thenReturn(null);

            worker.consumeMessages();

            verify(processor).process(eq(streamKey), eq(record), eq(0));
        }

        @Test
        @DisplayName("record가 없으면 processor.process()를 호출하지 않는다")
        void it_skips_processor_when_no_records() {
            when(redisAdapter.readMessages(anyString())).thenReturn(Collections.emptyList());

            worker.consumeMessages();

            verify(processor, never()).process(any(), any(), anyInt());
        }

        @Test
        @DisplayName("attempt 필드가 있으면 파싱하여 processor에 전달한다")
        void it_passes_attempt_to_processor() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of(
                            "messageId", "999",
                            "roomId", "42",
                            "senderId", "7",
                            "content", "hello",
                            "attempt", "3"
                    )
            ).withId(RecordId.of("1700000000000-0"));

            when(redisAdapter.readMessages(streamKey)).thenReturn(List.of(record));
            when(redisAdapter.readMessages(retryKey)).thenReturn(null);

            worker.consumeMessages();

            verify(processor).process(eq(streamKey), eq(record), eq(3));
        }
    }

    @Nested
    @DisplayName("reclaimPendingMessages()는")
    class Describe_reclaimPendingMessages {

        @Test
        @DisplayName("claimIdleRecords 결과를 processor에 위임한다")
        void it_delegates_claimed_records_to_processor() {
            MapRecord<String, String, String> record = MapRecord.create(
                    streamKey,
                    Map.of(
                            "messageId", "999",
                            "roomId", "42",
                            "senderId", "7",
                            "content", "hello"
                    )
            ).withId(RecordId.of("1700000000000-0"));

            when(redisAdapter.claimIdleRecords(streamKey)).thenReturn(List.of(record));
            when(redisAdapter.claimIdleRecords(retryKey)).thenReturn(Collections.emptyList());

            worker.reclaimPendingMessages();

            verify(processor).process(eq(streamKey), eq(record), eq(0));
        }
    }
}
