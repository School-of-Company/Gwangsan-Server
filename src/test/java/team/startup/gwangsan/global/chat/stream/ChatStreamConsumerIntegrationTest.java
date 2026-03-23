package team.startup.gwangsan.global.chat.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.service.SaveChatMessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Testcontainers
@DisplayName("ChatStreamConsumerWorker 통합 테스트")
class ChatStreamConsumerIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379);

    @Mock
    private SaveChatMessageService saveChatMessageService;

    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;
    private ChatStreamConsumerWorker worker;

    private static final String STREAM_KEY = "chat:room:1:messages";
    private static final String RETRY_KEY = "chat:retry:messages";
    private static final String DLQ_KEY = "chat:dlq:messages";

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379)
        );
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        ChatStreamProperties props = new ChatStreamProperties();
        props.setGroup("chat-message-persistors");
        props.setBatchSize(10);
        props.setBlockMs(100);
        props.setClaimIdleMs(30000);
        props.setRetryMax(5);
        props.setRetryKey(RETRY_KEY);
        props.setDlqKey(DLQ_KEY);

        ChatStreamRedisAdapter adapter = new ChatStreamRedisAdapter(redisTemplate, props);
        adapter.init();

        SaveChatMessageHandler handler = new SaveChatMessageHandler(saveChatMessageService);
        ChatStreamMessageProcessor processor = new ChatStreamMessageProcessor(List.of(handler), adapter, props);
        worker = new ChatStreamConsumerWorker(adapter, processor, props);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(STREAM_KEY);
        redisTemplate.delete(RETRY_KEY);
        redisTemplate.delete(DLQ_KEY);
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("stream에 메시지 발행 후 consumeMessages() 호출 시 서비스가 호출된다")
    void it_calls_service_from_stream() {
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(Map.of(
                                "messageId", "123456789",
                                "roomId", "1",
                                "senderId", "2",
                                "content", "통합 테스트 메시지",
                                "messageType", "TEXT"
                        ))
        );

        worker.consumeMessages();

        verify(saveChatMessageService).execute(eq(123456789L), eq(1L), eq("통합 테스트 메시지"), isNull(), eq(MessageType.TEXT), eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("roomId 없는 메시지는 DLQ로 이동하고 서비스가 호출되지 않는다")
    void it_sends_invalid_message_to_dlq() {
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(Map.of(
                                "messageId", "123456789",
                                "senderId", "2",
                                "content", "roomId 없음"
                        ))
        );

        worker.consumeMessages();

        verifyNoInteractions(saveChatMessageService);

        List<MapRecord<String, String, String>> dlqEntries =
                redisTemplate.<String, String>opsForStream().range(DLQ_KEY, Range.unbounded());
        assertThat(dlqEntries).isNotEmpty();
    }

    @Test
    @DisplayName("서비스 호출 실패 시 retry stream으로 이동한다")
    void it_sends_to_retry_on_service_failure() {
        doThrow(new RuntimeException("DB 오류"))
                .when(saveChatMessageService).execute(any(), any(), any(), any(), any(), any(), any());

        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(Map.of(
                                "messageId", "123456789",
                                "roomId", "1",
                                "senderId", "2",
                                "content", "저장 실패 메시지",
                                "messageType", "TEXT"
                        ))
        );

        worker.consumeMessages();

        List<MapRecord<String, String, String>> retryEntries =
                redisTemplate.<String, String>opsForStream().range(RETRY_KEY, Range.unbounded());
        assertThat(retryEntries).isNotEmpty();

        MapRecord<String, String, String> retryRecord = retryEntries.get(0);
        assertThat(retryRecord.getValue().get("content")).isEqualTo("저장 실패 메시지");
        assertThat(retryRecord.getValue().get("attempt")).isEqualTo("1");
    }

    @Test
    @DisplayName("imageIds 필드가 있으면 파싱되어 서비스에 전달된다")
    void it_passes_image_ids_to_service() {
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(Map.of(
                                "messageId", "123456789",
                                "roomId", "1",
                                "senderId", "2",
                                "content", "이미지 메시지",
                                "messageType", "TEXT",
                                "imageIds", "[10,20,30]"
                        ))
        );

        worker.consumeMessages();

        verify(saveChatMessageService).execute(eq(123456789L), eq(1L), eq("이미지 메시지"), eq(List.of(10L, 20L, 30L)), eq(MessageType.TEXT), eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("여러 메시지를 순서대로 처리한다")
    void it_processes_multiple_messages_in_order() {
        for (int i = 1; i <= 3; i++) {
            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(STREAM_KEY)
                            .ofMap(Map.of(
                                    "messageId", String.valueOf(100 + i),
                                    "roomId", "1",
                                    "senderId", "2",
                                    "content", "메시지 " + i,
                                    "messageType", "TEXT"
                            ))
            );
        }

        worker.consumeMessages();

        verify(saveChatMessageService, times(3)).execute(anyLong(), anyLong(), anyString(), isNull(), any(MessageType.class), anyLong(), any(LocalDateTime.class));
    }
}
