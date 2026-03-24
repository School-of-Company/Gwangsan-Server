package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.presentation.dto.response.MemberInfoResponse;
import team.startup.gwangsan.domain.auth.service.TokenAuthenticationService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationServiceImpl implements TokenAuthenticationService {

    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse execute() {
        Member member = memberUtil.getCurrentMember();
        log.info("[TokenAuthenticationService] Current Member: id={}, nickname={}", member.getId(), member.getNickname());
        return new MemberInfoResponse(
                member.getId(),
                member.getNickname()
        );
    }
}
