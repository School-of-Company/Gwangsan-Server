package team.startup.gwangsan.domain.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.auth.service.ReissueTokenService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReissueTokenServiceImpl implements ReissueTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public TokenResponse execute(String refreshToken) {
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(UnauthorizedException::new);

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException();
        }

        Member member = memberRepository.findByPhoneNumber(savedToken.getPhoneNumber())
                .orElseThrow(NotFoundUserException::new);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException();
        }

        String newAccessToken = jwtProvider.generateAccessToken(member.getPhoneNumber(), member.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken(member.getPhoneNumber());

        RefreshToken updatedToken = RefreshToken.builder()
                .phoneNumber(savedToken.getPhoneNumber())
                .token(newRefreshToken)
                .build();

        refreshTokenRepository.save(updatedToken);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                LocalDateTime.now().plusSeconds(jwtProvider.getAccessTokenTime()),
                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenTime())
        );
    }
}