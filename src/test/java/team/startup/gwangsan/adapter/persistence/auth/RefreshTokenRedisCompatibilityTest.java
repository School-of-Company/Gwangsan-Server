package team.startup.gwangsan.adapter.persistence.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.convert.Bucket;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.data.redis.core.mapping.RedisMappingContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshToken 은 domain.auth.entity 에서 adapter.persistence.auth 로 이동했다.
 * 운영 Redis 에는 이전 FQCN 을 _class 로 가진 hash 가 최대 7일 남아 있으므로 그대로 읽혀야 한다.
 */
@DisplayName("RefreshToken Redis 저장 호환성")
class RefreshTokenRedisCompatibilityTest {

    private static final String LEGACY_CLASS = "team.startup.gwangsan.domain.auth.entity.RefreshToken";

    private final MappingRedisConverter converter = converter();

    private static MappingRedisConverter converter() {
        MappingRedisConverter converter = new MappingRedisConverter(new RedisMappingContext());
        converter.afterPropertiesSet();
        return converter;
    }

    @Nested
    @DisplayName("이전 패키지로 저장된 hash 를")
    class Describe_legacy_hash {

        @Test
        @DisplayName("it_새 RefreshToken 으로 읽는다")
        void it_reads_legacy_class_hint() {
            RedisData legacy = new RedisData(Bucket.newBucketFromStringMap(Map.of(
                    "_class", LEGACY_CLASS,
                    "phoneNumber", "01012345678",
                    "token", "refresh")));
            legacy.setId("01012345678");

            RefreshToken token = converter.read(RefreshToken.class, legacy);

            assertThat(token.getPhoneNumber()).isEqualTo("01012345678");
            assertThat(token.getToken()).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("새로 저장하는 hash 는")
    class Describe_new_hash {

        @Test
        @DisplayName("it_keyspace·TTL·token 보조 인덱스를 그대로 유지한다")
        void it_keeps_keyspace_ttl_and_index() {
            RedisData data = new RedisData();
            converter.write(RefreshToken.builder().phoneNumber("01012345678").token("refresh").build(), data);

            assertThat(data.getKeyspace()).isEqualTo("gwangsan_refreshToken");
            assertThat(data.getId()).isEqualTo("01012345678");
            assertThat(data.getTimeToLive()).isEqualTo(60L * 60 * 24 * 7);
            assertThat(data.getIndexedData()).extracting(Object::toString)
                    .anySatisfy(index -> assertThat(index).contains("token"));
            assertThat(new String(data.getBucket().get("token"), StandardCharsets.UTF_8)).isEqualTo("refresh");
        }
    }
}
