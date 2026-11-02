package team.startup.gwangsan.adapter.stream.chat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("ChatStreamBacklogSampler 통합 테스트")
class ChatStreamBacklogSamplerIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final String GROUP = "chat-message-persistors";
    private static final String RETRY_KEY = "chat:retry:messages";

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private ChatStreamProperties props;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        connectionFactory = factory(redis.getMappedPort(6379));
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        props = new ChatStreamProperties();
        props.setGroup(GROUP);
        props.setRetryKey(RETRY_KEY);
        meters = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Nested
    @DisplayName("첫 snapshot 전에는")
    class Describe_before_sampling {

        @Test
        @DisplayName("it_값을 모름(NaN)과 실패/불완전(0)으로 둔다")
        void it_reports_unknown() {
            new ChatStreamBacklogSampler(redisTemplate, props, meters);

            assertThat(gauge("gwangsan.stream.unread", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.pending", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.snapshot.timestamp.seconds", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.snapshot.success", "rooms")).isZero();
            assertThat(gauge("gwangsan.stream.snapshot.complete", "rooms")).isZero();
        }
    }

    @Nested
    @DisplayName("모든 방 stream 을 훑고 lag 를 알면")
    class Describe_complete {

        @Test
        @DisplayName("it_lag 합을 unread, 미ACK 전달 수를 pending 으로 내고 complete=1 이다")
        void it_aggregates_rooms() {
            add("chat:room:1:messages", 3);
            add("chat:room:2:messages", 2);
            group("chat:room:1:messages");
            group("chat:room:2:messages");
            deliver("chat:room:1:messages", 1);
            ChatStreamBacklogSampler sampler = new ChatStreamBacklogSampler(redisTemplate, props, meters);

            sampler.sample();

            assertThat(gauge("gwangsan.stream.unread", "rooms")).isEqualTo(4);
            assertThat(gauge("gwangsan.stream.pending", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.success", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.complete", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.timestamp.seconds", "rooms"))
                    .isGreaterThan(System.currentTimeMillis() / 1000.0 - 60);
        }

        @Test
        @DisplayName("it_retry stream 이 아직 없으면 retry 는 0 으로 완전하다")
        void it_reports_empty_retry() {
            new ChatStreamBacklogSampler(redisTemplate, props, meters).sample();

            assertThat(gauge("gwangsan.stream.unread", "retry")).isZero();
            assertThat(gauge("gwangsan.stream.pending", "retry")).isZero();
            assertThat(gauge("gwangsan.stream.snapshot.complete", "retry")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("부분 정보만 있으면")
    class Describe_partial {

        @Test
        @DisplayName("it_Redis 가 lag 를 계산 못 하면(null) unread 는 NaN, pending 은 그대로, complete=0 이다")
        void it_hides_unknown_lag() {
            List<RecordId> ids = add("chat:room:1:messages", 3);
            group("chat:room:1:messages");
            deliver("chat:room:1:messages", 1);
            redisTemplate.opsForStream().delete("chat:room:1:messages", ids.get(1)); // 아직 안 읽은 항목 tombstone
            ChatStreamBacklogSampler sampler = new ChatStreamBacklogSampler(redisTemplate, props, meters);

            sampler.sample();

            assertThat(gauge("gwangsan.stream.unread", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.pending", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.success", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.complete", "rooms")).isZero();
        }

        @Test
        @DisplayName("it_consumer group 이 아직 없는 방이 있으면 unread 를 0 으로 채우지 않는다")
        void it_hides_unread_without_group() {
            add("chat:room:1:messages", 2);

            new ChatStreamBacklogSampler(redisTemplate, props, meters).sample();

            assertThat(gauge("gwangsan.stream.unread", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.snapshot.complete", "rooms")).isZero();
        }

        @Test
        @DisplayName("it_key 상한에 걸리면 부분 합을 내지 않고 complete=0 이다")
        void it_hides_truncated_sums() {
            add("chat:room:1:messages", 1);
            add("chat:room:2:messages", 1);
            group("chat:room:1:messages");
            group("chat:room:2:messages");
            props.setSnapshotMaxKeys(1);

            new ChatStreamBacklogSampler(redisTemplate, props, meters).sample();

            assertThat(gauge("gwangsan.stream.unread", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.pending", "rooms")).isNaN();
            assertThat(gauge("gwangsan.stream.snapshot.success", "rooms")).isEqualTo(1);
            assertThat(gauge("gwangsan.stream.snapshot.complete", "rooms")).isZero();
        }
    }

    @Nested
    @DisplayName("Redis 에 닿지 못하면")
    class Describe_failure {

        @Test
        @DisplayName("it_예외 없이 success=0, 값은 NaN 으로 둔다")
        void it_reports_failure() {
            LettuceConnectionFactory broken = factory(1);
            try {
                new ChatStreamBacklogSampler(new StringRedisTemplate(broken), props, meters).sample();
            } finally {
                broken.destroy();
            }

            assertThat(gauge("gwangsan.stream.snapshot.success", "rooms")).isZero();
            assertThat(gauge("gwangsan.stream.snapshot.success", "retry")).isZero();
            assertThat(gauge("gwangsan.stream.unread", "rooms")).isNaN();
        }
    }

    private double gauge(String name, String group) {
        return meters.get(name).tag("group", group).gauge().value();
    }

    private List<RecordId> add(String key, int count) {
        List<RecordId> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(redisTemplate.opsForStream().add(StreamRecords.newRecord().in(key).ofMap(Map.of("n", String.valueOf(i)))));
        }
        return ids;
    }

    private void group(String key) {
        redisTemplate.opsForStream().createGroup(key, ReadOffset.from("0"), GROUP);
    }

    private void deliver(String key, int count) {
        redisTemplate.opsForStream().read(Consumer.from(GROUP, "test"), StreamReadOptions.empty().count(count),
                StreamOffset.create(key, ReadOffset.lastConsumed()));
    }

    private static LettuceConnectionFactory factory(int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(redis.getHost(), port));
        factory.afterPropertiesSet();
        return factory;
    }
}
