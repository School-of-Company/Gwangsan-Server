package team.startup.gwangsan.compatibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import team.startup.gwangsan.adapter.redis.RedisUtil;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SMS 인증·로그아웃 blacklist 가 Redis 에 쓰는 값의 바이트를 고정한다.
 * 현재는 RedisUtil 이 Jackson JSON 으로 쓰므로 문자열이 따옴표를 포함한다("123456").
 * RedisUtil 을 좁은 Port/Adapter 로 바꿀 때 StringRedisTemplate 등으로 바꾸면 기존 키의 값을 못 읽는다.
 * 새 Adapter 로 교체한 PR 은 subject 생성만 새 Adapter 로 바꾸고 바이트 기대값은 그대로 둔다.
 * RedisTemplate 의 연결만 mock 으로 두고 실제 RedisUtil → RedisTemplate → serializer 경로를 실행한다.
 */
@DisplayName("Redis 값 저장 호환 (SMS 인증·blacklist)")
class RedisValueCompatibilityTest {

    private RedisConnection connection;
    private RedisUtil redisUtil;

    @BeforeEach
    void setUp() {
        connection = mock(RedisConnection.class);
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        given(factory.getConnection()).willReturn(connection);

        // RedisConfig.redisTemplate 과 같은 구성. blacklist 템플릿은 운영에서도 같은 빈이 주입된다.
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.afterPropertiesSet();
        redisUtil = new RedisUtil(template, template);
    }

    @Nested
    @DisplayName("쓰기는")
    class Describe_write {

        @Test
        @DisplayName("it_인증 코드 문자열을 JSON 문자열로 쓴다")
        void it_writes_code_as_json_string() {
            redisUtil.set("sms:code:01012345678", "123456", 180_000);

            assertThat(written("sms:code:01012345678", 180_000)).isEqualTo("\"123456\"");
        }

        @Test
        @DisplayName("it_시도 횟수·인증 완료를 JSON 숫자·불리언으로 쓴다")
        void it_writes_number_and_boolean() {
            redisUtil.set("sms:attempt:01012345678", 2, 180_000);
            redisUtil.set("sms:verified:01012345678", Boolean.TRUE, 600_000);

            assertThat(written("sms:attempt:01012345678", 180_000)).isEqualTo("2");
            assertThat(written("sms:verified:01012345678", 600_000)).isEqualTo("true");
        }

        @Test
        @DisplayName("it_로그아웃 access token 을 \"BLACKLIST\" JSON 문자열로 쓴다")
        void it_writes_blacklist() {
            redisUtil.setBlackList("access-jwt", "BLACKLIST", 3_600_000L);

            assertThat(written("access-jwt", 3_600_000L)).isEqualTo("\"BLACKLIST\"");
        }
    }

    @Nested
    @DisplayName("기존 값 읽기는")
    class Describe_read {

        @Test
        @DisplayName("it_baseline 이 쓴 바이트를 같은 타입으로 읽는다")
        void it_reads_baseline_bytes() {
            stored("sms:code:01012345678", "\"123456\"");
            stored("sms:attempt:01012345678", "2");
            stored("sms:verified:01012345678", "true");

            assertThat(redisUtil.get("sms:code:01012345678", String.class)).isEqualTo("123456");
            assertThat(redisUtil.get("sms:attempt:01012345678", Integer.class)).isEqualTo(2);
            assertThat(redisUtil.get("sms:verified:01012345678", Boolean.class)).isTrue();
        }
    }

    private String written(String key, long millis) {
        ArgumentCaptor<byte[]> value = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Expiration> ttl = ArgumentCaptor.forClass(Expiration.class);
        verify(connection).set(eq(bytes(key)), value.capture(), ttl.capture(), eq(SetOption.upsert()));
        assertThat(ttl.getValue().getExpirationTimeInMilliseconds()).isEqualTo(millis);
        return new String(value.getValue(), StandardCharsets.UTF_8);
    }

    private void stored(String key, String json) {
        given(connection.get(eq(bytes(key)))).willReturn(bytes(json));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
