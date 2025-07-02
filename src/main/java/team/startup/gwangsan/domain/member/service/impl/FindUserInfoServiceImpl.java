package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.FindUserInfoService;

@Service
@RequiredArgsConstructor
public class FindUserInfoServiceImpl implements FindUserInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public FindUserInfoResponse execute(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        MemberDetail detail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        return new FindUserInfoResponse(
                member.getId(),
                member.getNickname(),
                detail.getProfileUrl(),
                detail.getPlace().getName(),
                detail.getLight()
        );
    }
}

