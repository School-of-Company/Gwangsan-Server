package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;
import team.startup.gwangsan.domain.admin.service.SignInAdminService;
import team.startup.gwangsan.adapter.persistence.auth.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.adapter.web.auth.dto.response.TokenResponse;
import team.startup.gwangsan.adapter.persistence.auth.RefreshTokenRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignInAdminServiceImpl implements SignInAdminService {

    private final MemberJpaRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public SignInAdminResponse execute(String nickname, String password) {
        MemberJpaEntity member = memberRepository.findByNickname(nickname)
                .orElseThrow(NotFoundMemberException::new);

        validateMemberRole(member);

        validateMemberPassword(password, member);
        validateMemberStatus(member);

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

    private void validateMemberRole(MemberJpaEntity member) {
        if (member.getRole() == MemberRole.ROLE_USER) {
            throw new ForbiddenException();
        }
    }

    private void validateMemberStatus(MemberJpaEntity member) {
        if (member.getStatus() == MemberStatus.PENDING) {
            throw new PendingApprovalException();
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException();
        }
    }

    private void validateMemberPassword(String password, MemberJpaEntity member) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new UnauthorizedException();
        }
    }
}
