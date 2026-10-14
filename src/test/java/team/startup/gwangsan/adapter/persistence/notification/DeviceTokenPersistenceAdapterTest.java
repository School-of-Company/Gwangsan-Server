package team.startup.gwangsan.adapter.persistence.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.convert.Bucket;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DeviceToken Redis 저장")
class DeviceTokenPersistenceAdapterTest {

    private static final String LEGACY_CLASS = "team.startup.gwangsan.domain.notification.entity.DeviceToken";

    @Nested
    @DisplayName("DeviceTokenPersistenceAdapter 는")
    class Describe_adapter {

        private final DeviceTokenRepository repository = mock(DeviceTokenRepository.class);
        private final DeviceTokenPersistenceAdapter adapter = new DeviceTokenPersistenceAdapter(repository);

        @Test
        @DisplayName("회원 순서대로 토큰 문자열을 모으고 null 토큰은 뺀다")
        void it_loads_tokens_in_member_order() {
            when(repository.findAllByUserId(1L)).thenReturn(List.of(token("d1", 1L, "t1"), token("d2", 1L, null)));
            when(repository.findAllByUserId(2L)).thenReturn(List.of(token("d3", 2L, "t3")));

            assertThat(adapter.loadTokens(List.of(1L, 2L))).containsExactly("t1", "t3");
        }

        @Test
        @DisplayName("기기 토큰을 기존 필드 그대로 저장한다")
        void it_saves_device_token() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            adapter.save("device", 3L, "tok", OsType.IOS);

            verify(repository).save(argThat(t -> t.getDeviceId().equals("device") && t.getUserId().equals(3L)
                    && t.getDeviceToken().equals("tok") && t.getOsType() == OsType.IOS));
        }
    }

    @Nested
    @DisplayName("Redis 매핑은 패키지 이동 전 hash 와 호환된다")
    class Describe_redis_compatibility {

        private final MappingRedisConverter converter = converter();

        @Test
        @DisplayName("이동 전 _class 값을 가진 hash 를 읽는다")
        void it_reads_hash_written_before_move() {
            Bucket bucket = new Bucket();
            bucket.put("_class", bytes(LEGACY_CLASS));
            bucket.put("userId", bytes("7"));
            bucket.put("deviceToken", bytes("ExponentPushToken[x]"));
            bucket.put("osType", bytes("ANDROID"));
            RedisData data = new RedisData(bucket);
            data.setId("device-1");
            data.setKeyspace("gwangsan_deviceToken");

            DeviceToken read = converter.read(DeviceToken.class, data);

            assertThat(read.getDeviceId()).isEqualTo("device-1");
            assertThat(read.getUserId()).isEqualTo(7L);
            assertThat(read.getDeviceToken()).isEqualTo("ExponentPushToken[x]");
            assertThat(read.getOsType()).isEqualTo(OsType.ANDROID);
        }

        @Test
        @DisplayName("새로 쓰는 hash 도 이동 전과 같은 keyspace·_class·TTL·userId 인덱스를 쓴다")
        void it_writes_same_layout() {
            RedisData data = new RedisData();
            converter.write(new DeviceToken("device-1", 7L, "tok", OsType.IOS), data);

            assertThat(data.getKeyspace()).isEqualTo("gwangsan_deviceToken");
            assertThat(new String(data.getBucket().get("_class"), StandardCharsets.UTF_8)).isEqualTo(LEGACY_CLASS);
            assertThat(new String(data.getBucket().get("userId"), StandardCharsets.UTF_8)).isEqualTo("7");
            assertThat(data.getTimeToLive()).isEqualTo(60L * 60 * 24 * 30);
            assertThat(data.getIndexedData()).anyMatch(i -> i.getIndexName().equals("userId"));
        }

        private static MappingRedisConverter converter() {
            MappingRedisConverter converter = new MappingRedisConverter(new RedisMappingContext());
            converter.afterPropertiesSet();
            return converter;
        }
    }

    private static DeviceToken token(String deviceId, Long userId, String value) {
        return DeviceToken.builder().deviceId(deviceId).userId(userId).deviceToken(value).osType(OsType.IOS).build();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
