package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.auth.service.SignOutService;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import team.startup.gwangsan.global.util.MemberUtil;
import team.startup.gwangsan.global.redis.RedisUtil;

@Service
@RequiredArgsConstructor
public class SignOutServiceImpl implements SignOutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberUtil memberUtil;
    private final RedisUtil redisUtil;
    private final JwtProvider jwtProvider;

    @Override
    public void execute(String accessToken) {
        String phoneNumber = memberUtil.getCurrentMember().getPhoneNumber();
        refreshTokenRepository.deleteById(phoneNumber);

        Long expirationMillis = jwtProvider.getExpiration(accessToken);
        redisUtil.setBlackList(accessToken, "BLACKLIST", expirationMillis);
    }
}
