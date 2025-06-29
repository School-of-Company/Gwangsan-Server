package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.entity.DeviceToken;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.auth.service.SignInService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements SignInService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public TokenResponse execute(SignInRequest request) {
        Member member = memberRepository.findByNickname(request.nickname())
                .orElseThrow(NotFoundUserException::new);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException();
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new UnauthorizedException();
        }

        String accessToken = jwtProvider.generateAccessToken(member.getPhoneNumber(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getPhoneNumber());

        refreshTokenRepository.save(RefreshToken.builder()
                .phoneNumber(member.getPhoneNumber())
                .token(refreshToken)
                .build());

        deviceTokenRepository.save(DeviceToken.builder()
                .deviceId(request.deviceId())
                .userId(member.getId())
                .deviceToken(request.deviceToken())
                .osType(request.osType())
                .build());

        return new TokenResponse(
                accessToken,
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtProvider.getAccessTokenTime()),
                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenTime())
        );
    }
}
