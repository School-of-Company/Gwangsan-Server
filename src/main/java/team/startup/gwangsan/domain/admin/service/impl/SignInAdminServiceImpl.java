package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;
import team.startup.gwangsan.domain.admin.service.SignInAdminService;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignInAdminServiceImpl implements SignInAdminService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public SignInAdminResponse execute(String nickname, String password) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(NotFoundMemberException::new);

        validateMemberRole(member);

        validateMemberStatus(member);
        validateMemberPassword(password, member);

        String accessToken = jwtProvider.generateAccessToken(member.getPhoneNumber(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getPhoneNumber());

        refreshTokenRepository.save(RefreshToken.builder()
                .phoneNumber(member.getPhoneNumber())
                .token(refreshToken)
                .build());

        TokenResponse token = new TokenResponse(
                accessToken,
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtProvider.getAccessTokenTime()),
                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenTime())
        );

        return new SignInAdminResponse(
                token,
                member.getRole()
        );
    }

    private void validateMemberRole(Member member) {
        if (member.getRole() == MemberRole.ROLE_USER) {
            throw new ForbiddenException();
        }
    }

    private void validateMemberStatus(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException();
        }
    }

    private void validateMemberPassword(String password, Member member) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new UnauthorizedException();
        }
    }
}
