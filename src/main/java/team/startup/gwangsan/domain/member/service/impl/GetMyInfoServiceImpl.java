package team.startup.gwangsan.domain.member.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.GetMyInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.GetMyInfoService;
import team.startup.gwangsan.global.security.exception.InvalidTokenException;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

@Service
@RequiredArgsConstructor
public class GetMyInfoServiceImpl implements GetMyInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional(readOnly = true)
    public GetMyInfoResponse execute(HttpServletRequest request) {
        String token = resolveToken(request);
        String phoneNumber = jwtProvider.validateAndGetSubject(token);

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(NotFoundMemberException::new);

        MemberDetail detail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberException::new);

        return new GetMyInfoResponse(
                member.getId(),
                member.getNickname(),
                detail.getProfileUrl(),
                detail.getLight(),
                detail.getGwangsan()
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        throw new InvalidTokenException();
    }
}