package team.startup.gwangsan.adapter.redis.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.TokenBlacklistPort;
import team.startup.gwangsan.global.redis.RedisUtil;

/** 로그아웃한 access token 을 남은 만료시간 동안 블랙리스트에 둔다. 키는 토큰 원문(기존 형식). */
@Component
@RequiredArgsConstructor
public class TokenBlacklistRedisAdapter implements TokenBlacklistPort {

    private final RedisUtil redisUtil;

    @Override
    public void add(String accessToken, long expirationMillis) {
        redisUtil.setBlackList(accessToken, "BLACKLIST", expirationMillis);
    }
}
