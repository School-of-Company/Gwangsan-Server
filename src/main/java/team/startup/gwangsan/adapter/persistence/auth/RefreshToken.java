package team.startup.gwangsan.adapter.persistence.auth;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;

@RedisHash(value = "gwangsan_refreshToken", timeToLive = JwtProvider.REFRESH_TOKEN_TIME)
@Getter
@Builder
public class RefreshToken {

    @Id
    private String phoneNumber;

    @Indexed
    private String token;
}
