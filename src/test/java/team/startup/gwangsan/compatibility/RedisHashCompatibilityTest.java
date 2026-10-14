package team.startup.gwangsan.compatibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.convert.Bucket;
import org.springframework.data.redis.core.convert.KeyspaceConfiguration;
import org.springframework.data.redis.core.convert.MappingConfiguration;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.data.redis.core.convert.SimpleIndexedPropertyValue;
import org.springframework.data.redis.core.index.IndexConfiguration;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import team.startup.gwangsan.adapter.persistence.auth.RefreshToken;
import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 Redis 에 이미 저장된 @RedisHash 값(RefreshToken 7일, DeviceToken 30일)이 클래스 이동 뒤에도 읽히는지 고정한다.
 * Spring Data Redis 는 hash 에 _class(FQCN) 를 기록한다. 기준 hash 는 baseline 의 FQCN 을 그대로 담고 있으므로,
 * 클래스를 옮긴 PR 은 아래 import 만 새 클래스로 바꾸고 LEGACY_* 상수와 hash 내용은 바꾸지 않는다.
 * Redis 연결 없이 repository 가 쓰는 MappingRedisConverter 로 쓰기/읽기를 검증한다.
 */
@DisplayName("Redis @RedisHash 저장 호환")
class RedisHashCompatibilityTest {

    // baseline develop 474a2cde 가 운영 Redis 에 기록한 _class 값. 이동 후에도 바꾸지 않는다.
    private static final String LEGACY_REFRESH_TOKEN_CLASS = "team.startup.gwangsan.domain.auth.entity.RefreshToken";
    private static final String LEGACY_DEVICE_TOKEN_CLASS = "team.startup.gwangsan.domain.notification.entity.DeviceToken";

    private MappingRedisConverter converter;

    @BeforeEach
    void setUp() {
        RedisMappingContext context = new RedisMappingContext(
                new MappingConfiguration(new IndexConfiguration(), new KeyspaceConfiguration()));
        converter = new MappingRedisConverter(context, null, null);
        converter.afterPropertiesSet();
    }

    @Nested
    @DisplayName("RefreshToken 은")
    class Describe_refresh_token {

        @Test
        @DisplayName("it_기존 keyspace·필드·index·TTL 로 쓴다")
        void it_writes_baseline_layout() {
            RedisData data = new RedisData();
            converter.write(RefreshToken.builder().phoneNumber("01012345678").token("refresh-jwt").build(), data);

            assertThat(data.getKeyspace()).isEqualTo("gwangsan_refreshToken");
            assertThat(data.getId()).isEqualTo("01012345678");
            assertThat(data.getTimeToLive()).isEqualTo(604800L);
            assertThat(fields(data.getBucket())).containsExactlyEntriesOf(new TreeMap<>(Map.of(
                    "phoneNumber", "01012345678",
                    "token", "refresh-jwt")));
            assertThat(indexes(data)).containsExactly("gwangsan_refreshToken:token:refresh-jwt");
        }

        @Test
        @DisplayName("it_baseline FQCN 이 기록된 hash 를 읽는다")
        void it_reads_legacy_hash() {
            RefreshToken token = converter.read(RefreshToken.class, legacyHash("gwangsan_refreshToken", "01012345678", Map.of(
                    "_class", LEGACY_REFRESH_TOKEN_CLASS,
                    "phoneNumber", "01012345678",
                    "token", "refresh-jwt")));

            assertThat(token.getPhoneNumber()).isEqualTo("01012345678");
            assertThat(token.getToken()).isEqualTo("refresh-jwt");
        }

        @Test
        @DisplayName("it__class 가 더 이상 없는 클래스여도 선언 타입으로 읽는다 (이동 후 상황 재현)")
        void it_reads_hash_whose_class_no_longer_exists() {
            RefreshToken token = converter.read(RefreshToken.class, legacyHash("gwangsan_refreshToken", "01012345678", Map.of(
                    "_class", "team.startup.gwangsan.removed.RefreshToken",
                    "phoneNumber", "01012345678",
                    "token", "refresh-jwt")));

            assertThat(token.getToken()).isEqualTo("refresh-jwt");
        }
    }

    @Nested
    @DisplayName("DeviceToken 은")
    class Describe_device_token {

        @Test
        @DisplayName("it_기존 keyspace·필드·index·TTL 로 쓴다 (enum 은 이름)")
        void it_writes_baseline_layout() {
            RedisData data = new RedisData();
            converter.write(new DeviceToken("device-1", 7L, "ExponentPushToken[abc]", OsType.IOS), data);

            assertThat(data.getKeyspace()).isEqualTo("gwangsan_deviceToken");
            assertThat(data.getId()).isEqualTo("device-1");
            assertThat(data.getTimeToLive()).isEqualTo(2592000L);
            assertThat(fields(data.getBucket())).containsExactlyEntriesOf(new TreeMap<>(Map.of(
                    "deviceId", "device-1",
                    "userId", "7",
                    "deviceToken", "ExponentPushToken[abc]",
                    "osType", "IOS")));
            assertThat(indexes(data)).containsExactly("gwangsan_deviceToken:userId:7");
        }

        @Test
        @DisplayName("it_baseline FQCN 이 기록된 hash 를 읽는다")
        void it_reads_legacy_hash() {
            DeviceToken token = converter.read(DeviceToken.class, legacyHash("gwangsan_deviceToken", "device-1", Map.of(
                    "_class", LEGACY_DEVICE_TOKEN_CLASS,
                    "deviceId", "device-1",
                    "userId", "7",
                    "deviceToken", "ExponentPushToken[abc]",
                    "osType", "ANDROID")));

            assertThat(token.getDeviceId()).isEqualTo("device-1");
            assertThat(token.getUserId()).isEqualTo(7L);
            assertThat(token.getDeviceToken()).isEqualTo("ExponentPushToken[abc]");
            assertThat(token.getOsType()).isEqualTo(OsType.ANDROID);
        }
    }

    private static RedisData legacyHash(String keyspace, String id, Map<String, String> hash) {
        Bucket bucket = new Bucket();
        hash.forEach((k, v) -> bucket.put(k, v.getBytes(StandardCharsets.UTF_8)));
        RedisData data = new RedisData(bucket);
        data.setKeyspace(keyspace);
        data.setId(id);
        return data;
    }

    // _class 는 쓰는 클래스의 FQCN 이므로 이동 후 값이 달라지는 것이 정상이다. 읽기 호환은 it_reads_legacy_hash 가 본다.
    private static Map<String, String> fields(Bucket bucket) {
        return bucket.asMap().entrySet().stream()
                .filter(e -> !e.getKey().equals("_class"))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new String(e.getValue(), StandardCharsets.UTF_8),
                        (a, b) -> a, TreeMap::new));
    }

    // Redis 에 만들어지는 secondary index set 키: <keyspace>:<property>:<value>
    private static List<String> indexes(RedisData data) {
        return data.getIndexedData().stream()
                .map(index -> index.getKeyspace() + ":" + index.getIndexName() + ":"
                        + ((SimpleIndexedPropertyValue) index).getValue())
                .toList();
    }
}
